package sg.tracker.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Polls LTA on a fixed interval, and once at startup so the dashboard has data immediately. */
@Component
public class PollingScheduler {

    private final DisruptionService service;

    public PollingScheduler(DisruptionService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void pollOnStartup() {
        service.refresh();
    }

    @Scheduled(fixedDelayString = "${lta.poll-interval-ms}", initialDelayString = "${lta.poll-interval-ms}")
    public void poll() {
        service.refresh();
    }
}
