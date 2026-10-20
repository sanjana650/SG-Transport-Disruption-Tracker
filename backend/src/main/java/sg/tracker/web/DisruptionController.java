package sg.tracker.web;

import org.springframework.web.bind.annotation.*;
import sg.tracker.model.Disruption;
import sg.tracker.service.DisruptionService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DisruptionController {

    private final DisruptionService service;

    public DisruptionController(DisruptionService service) {
        this.service = service;
    }

    /** List disruptions. ?line=EWL filters by line, ?activeOnly=false includes resolved history. */
    @GetMapping("/disruptions")
    public List<DisruptionDto> list(@RequestParam(required = false) String line,
                                    @RequestParam(defaultValue = "true") boolean activeOnly) {
        return service.find(line, activeOnly).stream().map(DisruptionDto::from).toList();
    }

    /** Overall status plus active count per line. */
    @GetMapping("/status")
    public StatusDto status() {
        Map<String, Long> byLine = service.activeCountByLine();
        long total = byLine.values().stream().mapToLong(Long::longValue).sum();
        return new StatusDto(total == 0 ? "NORMAL" : "DISRUPTED", total, byLine);
    }

    /** Force an immediate re-poll of LTA (used by the dashboard's Refresh button). */
    @PostMapping("/refresh")
    public StatusDto refresh() {
        service.refresh();
        return status();
    }

    public record StatusDto(String overall, long activeCount, Map<String, Long> activeByLine) {
    }

    public record DisruptionDto(Long id, String line, String direction, String stations, String message,
                                String severity, boolean freeBusAvailable, boolean active,
                                Instant firstSeen, Instant lastSeen, Instant resolvedAt) {
        static DisruptionDto from(Disruption d) {
            return new DisruptionDto(d.getId(), d.getLine(), d.getDirection(), d.getStations(), d.getMessage(),
                    d.getSeverity().name(), d.isFreeBusAvailable(), d.isActive(),
                    d.getFirstSeen(), d.getLastSeen(), d.getResolvedAt());
        }
    }
}
