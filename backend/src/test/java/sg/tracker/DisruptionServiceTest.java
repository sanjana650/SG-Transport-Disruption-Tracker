package sg.tracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import sg.tracker.client.LtaClient;
import sg.tracker.client.LtaTrainAlerts;
import sg.tracker.client.LtaTrainAlerts.Message;
import sg.tracker.client.LtaTrainAlerts.Segment;
import sg.tracker.client.LtaTrainAlerts.Value;
import sg.tracker.model.Disruption;
import sg.tracker.model.DisruptionRepository;
import sg.tracker.model.Severity;
import sg.tracker.service.DisruptionService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
class DisruptionServiceTest {

    @Autowired
    DisruptionRepository repository;

    LtaClient client;
    DisruptionService service;

    @BeforeEach
    void setUp() {
        client = mock(LtaClient.class);
        Clock clock = Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneOffset.UTC);
        service = new DisruptionService(client, repository, clock);
    }

    private static LtaTrainAlerts disrupted(String message, Segment... segments) {
        return new LtaTrainAlerts(new Value(2, List.of(segments), List.of(new Message(message, "2026-10-20 08:00:00"))));
    }

    private static LtaTrainAlerts normal() {
        return new LtaTrainAlerts(new Value(1, List.of(), List.of()));
    }

    private static Segment seg(String line, String direction, String stations, String bus) {
        return new Segment(line, direction, stations, bus, "", "");
    }

    @Test
    void normalServiceCreatesNoDisruptions() {
        when(client.fetchTrainAlerts()).thenReturn(normal());

        assertThat(service.refresh()).isZero();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void disruptionIsStoredAsActive() {
        when(client.fetchTrainAlerts()).thenReturn(
                disrupted("EWL: No train service between Boon Lay and Jurong East.",
                        seg("EWL", "Both", "EW27,EW26", "EW27")));

        assertThat(service.refresh()).isEqualTo(1);

        List<Disruption> all = repository.findAll();
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getLine()).isEqualTo("EWL");
        assertThat(all.get(0).getSeverity()).isEqualTo(Severity.MAJOR);
        assertThat(all.get(0).isActive()).isTrue();
        assertThat(all.get(0).isFreeBusAvailable()).isTrue();
    }

    @Test
    void sameDisruptionOnNextPollIsNotDuplicated() {
        when(client.fetchTrainAlerts()).thenReturn(
                disrupted("EWL: Delay of 10 minutes.", seg("EWL", "Both", "EW27,EW26", "")));

        service.refresh();
        service.refresh();

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void disruptionIsResolvedWhenItDisappears() {
        when(client.fetchTrainAlerts())
                .thenReturn(disrupted("EWL: Delay of 10 minutes.", seg("EWL", "Both", "EW27,EW26", "")))
                .thenReturn(normal());

        service.refresh();
        service.refresh();

        Disruption d = repository.findAll().get(0);
        assertThat(d.isActive()).isFalse();
        assertThat(d.getResolvedAt()).isNotNull();
        assertThat(service.find(null, true)).isEmpty();
        assertThat(service.find(null, false)).hasSize(1);
    }

    @Test
    void apiFailureKeepsExistingDataInsteadOfResolvingEverything() {
        when(client.fetchTrainAlerts())
                .thenReturn(disrupted("EWL: Delay of 10 minutes.", seg("EWL", "Both", "EW27,EW26", "")))
                .thenThrow(new RuntimeException("LTA is down"));

        service.refresh();
        int activeAfterFailure = service.refresh();

        assertThat(activeAfterFailure).isEqualTo(1);
        assertThat(repository.findByActiveTrue()).hasSize(1);
    }

    @Test
    void severityIsWorkedOutPerLine() {
        String message = "EWL: No train service between A and B. NSL: Delay of 5 minutes towards Marina South Pier.";
        when(client.fetchTrainAlerts()).thenReturn(disrupted(message,
                seg("EWL", "Both", "EW1", ""),
                seg("NSL", "Northbound", "NS1", "")));

        service.refresh();

        assertThat(service.find("EWL", true).get(0).getSeverity()).isEqualTo(Severity.MAJOR);
        assertThat(service.find("nsl", true).get(0).getSeverity()).isEqualTo(Severity.MINOR);
    }

    @Test
    void activeCountByLineGroupsCorrectly() {
        when(client.fetchTrainAlerts()).thenReturn(disrupted("EWL: Delay. NSL: Delay.",
                seg("EWL", "Both", "EW1", ""),
                seg("EWL", "Westbound", "EW5", ""),
                seg("NSL", "Northbound", "NS1", "")));

        service.refresh();

        assertThat(service.activeCountByLine()).isEqualTo(Map.of("EWL", 2L, "NSL", 1L));
    }

    @Test
    void classifyHandlesMissingText() {
        assertThat(DisruptionService.classify(null, "EWL")).isEqualTo(Severity.INFO);
        assertThat(DisruptionService.classify("", "EWL")).isEqualTo(Severity.INFO);
    }
}
