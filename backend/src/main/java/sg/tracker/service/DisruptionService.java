package sg.tracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.tracker.client.LtaClient;
import sg.tracker.client.LtaTrainAlerts;
import sg.tracker.model.Disruption;
import sg.tracker.model.DisruptionRepository;
import sg.tracker.model.Severity;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core logic: pull alerts from LTA, turn them into Disruption rows, and keep track of
 * which ones are still active. A disruption that was active last poll but is missing
 * from this poll is marked resolved.
 */
@Service
public class DisruptionService {

    private static final Logger log = LoggerFactory.getLogger(DisruptionService.class);

    private final LtaClient ltaClient;
    private final DisruptionRepository repository;
    private final Clock clock;

    public DisruptionService(LtaClient ltaClient, DisruptionRepository repository, Clock clock) {
        this.ltaClient = ltaClient;
        this.repository = repository;
        this.clock = clock;
    }

    /** Fetch from LTA and sync the database. Returns the number of currently active disruptions. */
    @Transactional
    public int refresh() {
        LtaTrainAlerts alerts;
        try {
            alerts = ltaClient.fetchTrainAlerts();
        } catch (RuntimeException e) {
            // Keep the previous data rather than wrongly marking everything resolved.
            log.warn("Could not fetch LTA alerts, keeping existing data: {}", e.getMessage());
            return repository.findByActiveTrue().size();
        }

        Instant now = clock.instant();
        List<Disruption> parsed = parse(alerts, now);
        Set<String> seen = new HashSet<>();

        for (Disruption incoming : parsed) {
            seen.add(incoming.getFingerprint());
            Optional<Disruption> existing = repository.findFirstByFingerprintAndActiveTrue(incoming.getFingerprint());
            if (existing.isPresent()) {
                existing.get().refresh(incoming.getMessage(), incoming.getSeverity(),
                        incoming.isFreeBusAvailable(), now);
            } else {
                repository.save(incoming);
            }
        }

        for (Disruption active : repository.findByActiveTrue()) {
            if (!seen.contains(active.getFingerprint())) {
                active.resolve(now);
            }
        }
        return seen.size();
    }

    /** Turn the raw LTA response into Disruption objects (not yet saved). */
    List<Disruption> parse(LtaTrainAlerts alerts, Instant now) {
        if (alerts == null || alerts.value() == null || alerts.value().status() != 2
                || alerts.value().affectedSegments() == null) {
            return List.of();
        }
        String message = joinMessages(alerts.value().messages());
        List<Disruption> result = new ArrayList<>();
        for (LtaTrainAlerts.Segment seg : alerts.value().affectedSegments()) {
            if (seg.line() == null || seg.line().isBlank()) {
                continue;
            }
            String fingerprint = String.join("|", seg.line(), nullToEmpty(seg.direction()), nullToEmpty(seg.stations()));
            boolean freeBus = !nullToEmpty(seg.freePublicBus()).isBlank();
            result.add(new Disruption(fingerprint, seg.line(), seg.direction(), seg.stations(),
                    message, classify(message, seg.line()), freeBus, now));
        }
        return result;
    }

    /**
     * Work out severity from the sentence about this line. "No train service" = MAJOR,
     * "delay" = MINOR, anything else = INFO.
     */
    static Severity classify(String message, String line) {
        String relevant = sentenceFor(message, line).toLowerCase(Locale.ROOT);
        if (relevant.contains("no train service") || relevant.contains("no service")) {
            return Severity.MAJOR;
        }
        if (relevant.contains("delay")) {
            return Severity.MINOR;
        }
        return Severity.INFO;
    }

    /** LTA sends one blob of text covering several lines ("EWL: ... NSL: ..."); pick this line's part. */
    static String sentenceFor(String message, String line) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String marker = line.toUpperCase(Locale.ROOT) + ":";
        String upper = message.toUpperCase(Locale.ROOT);
        int start = upper.indexOf(marker);
        if (start < 0) {
            return message;
        }
        int end = message.length();
        // Stop at the next "XXL:" style marker, if any.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b[A-Z]{2,3}:").matcher(message);
        while (m.find()) {
            if (m.start() > start) {
                end = m.start();
                break;
            }
        }
        return message.substring(start, end).trim();
    }

    private static String joinMessages(List<LtaTrainAlerts.Message> messages) {
        if (messages == null) {
            return "";
        }
        return messages.stream()
                .map(LtaTrainAlerts.Message::content)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    @Transactional(readOnly = true)
    public List<Disruption> find(String line, boolean activeOnly) {
        boolean hasLine = line != null && !line.isBlank();
        if (activeOnly) {
            return hasLine ? repository.findByActiveTrueAndLineIgnoreCase(line) : repository.findByActiveTrue();
        }
        return hasLine ? repository.findByLineIgnoreCaseOrderByFirstSeenDesc(line)
                : repository.findAllByOrderByFirstSeenDesc();
    }

    /** Count of active disruptions per line, e.g. {EWL=1, NSL=1}. */
    @Transactional(readOnly = true)
    public Map<String, Long> activeCountByLine() {
        return repository.findByActiveTrue().stream()
                .collect(Collectors.groupingBy(Disruption::getLine, TreeMap::new, Collectors.counting()));
    }
}
