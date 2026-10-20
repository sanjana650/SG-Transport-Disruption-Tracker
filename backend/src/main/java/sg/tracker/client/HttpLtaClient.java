package sg.tracker.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sg.tracker.LtaProperties;

/** Calls the real LTA DataMall API. Needs an AccountKey (free from LTA DataMall). */
@Component
@ConditionalOnProperty(name = "lta.mock-enabled", havingValue = "false")
public class HttpLtaClient implements LtaClient {

    private final RestClient restClient;

    public HttpLtaClient(LtaProperties props) {
        if (props.accountKey() == null || props.accountKey().isBlank()) {
            throw new IllegalStateException("LTA_ACCOUNT_KEY must be set when LTA_MOCK_ENABLED=false");
        }
        this.restClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("AccountKey", props.accountKey())
                .defaultHeader("accept", "application/json")
                .build();
    }

    @Override
    public LtaTrainAlerts fetchTrainAlerts() {
        return restClient.get()
                .uri("/TrainServiceAlerts")
                .retrieve()
                .body(LtaTrainAlerts.class);
    }
}
