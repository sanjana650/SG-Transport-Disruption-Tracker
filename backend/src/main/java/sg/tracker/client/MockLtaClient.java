package sg.tracker.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sample data in the same shape as the real API, so the app runs without an API key.
 * Cycles through scenarios on each poll so you can watch disruptions appear and resolve:
 * 1) one major disruption, 2) two disruptions, 3) everything back to normal.
 */
@Component
@ConditionalOnProperty(name = "lta.mock-enabled", havingValue = "true", matchIfMissing = true)
public class MockLtaClient implements LtaClient {

    private final AtomicInteger calls = new AtomicInteger();

    @Override
    public LtaTrainAlerts fetchTrainAlerts() {
        return switch (calls.getAndIncrement() % 3) {
            case 0 -> eastWestDisruption();
            case 1 -> eastWestAndNorthSouthDisruption();
            default -> normalService();
        };
    }

    private LtaTrainAlerts normalService() {
        return new LtaTrainAlerts(new LtaTrainAlerts.Value(1, List.of(), List.of()));
    }

    private LtaTrainAlerts eastWestDisruption() {
        var seg = new LtaTrainAlerts.Segment("EWL", "Both", "EW27,EW26,EW25,EW24",
                "EW27,EW26,EW25,EW24", "EW27,EW26,EW25,EW24", "Both");
        var msg = new LtaTrainAlerts.Message(
                "EWL: No train service between Boon Lay and Jurong East due to a signalling fault. "
                        + "Free regular bus and shuttle services available.",
                "2026-10-20 08:12:00");
        return new LtaTrainAlerts(new LtaTrainAlerts.Value(2, List.of(seg), List.of(msg)));
    }

    private LtaTrainAlerts eastWestAndNorthSouthDisruption() {
        var ew = new LtaTrainAlerts.Segment("EWL", "Both", "EW27,EW26,EW25,EW24", "", "", "");
        var ns = new LtaTrainAlerts.Segment("NSL", "Northbound", "NS13,NS14", "", "", "");
        var msg = new LtaTrainAlerts.Message(
                "EWL: Delay of 10 minutes between Boon Lay and Jurong East. "
                        + "NSL: Delay of 5 minutes towards Marina South Pier due to a train fault.",
                "2026-10-20 08:30:00");
        return new LtaTrainAlerts(new LtaTrainAlerts.Value(2, List.of(ew, ns), List.of(msg)));
    }
}
