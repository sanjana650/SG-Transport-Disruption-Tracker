package sg.tracker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lta")
public record LtaProperties(boolean mockEnabled, String baseUrl, String accountKey, long pollIntervalMs) {
}
