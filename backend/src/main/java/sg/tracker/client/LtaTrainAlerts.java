package sg.tracker.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Shape of the LTA DataMall "TrainServiceAlerts" response.
 * Status 1 = normal service, 2 = disrupted.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LtaTrainAlerts(@JsonProperty("value") Value value) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(
            @JsonProperty("Status") int status,
            @JsonProperty("AffectedSegments") List<Segment> affectedSegments,
            @JsonProperty("Message") List<Message> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            @JsonProperty("Line") String line,
            @JsonProperty("Direction") String direction,
            @JsonProperty("Stations") String stations,
            @JsonProperty("FreePublicBus") String freePublicBus,
            @JsonProperty("FreeMRTShuttle") String freeMrtShuttle,
            @JsonProperty("MRTShuttleDirection") String mrtShuttleDirection) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("Content") String content,
            @JsonProperty("CreatedDate") String createdDate) {
    }
}
