package sg.tracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import sg.tracker.client.LtaTrainAlerts;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks we can read a response shaped like the real LTA DataMall TrainServiceAlerts API. */
class LtaResponseParsingTest {

    private static final String SAMPLE = """
            {
              "odata.metadata": "https://datamall2.mytransport.sg/ltaodataservice/$metadata#TrainServiceAlerts",
              "value": {
                "Status": 2,
                "AffectedSegments": [
                  {
                    "Line": "EWL",
                    "Direction": "Both",
                    "Stations": "EW27,EW26",
                    "FreePublicBus": "EW27,EW26",
                    "FreeMRTShuttle": "",
                    "MRTShuttleDirection": ""
                  }
                ],
                "Message": [
                  { "Content": "EWL: No train service between Boon Lay and Jurong East.", "CreatedDate": "2026-10-20 08:12:00" }
                ]
              }
            }
            """;

    @Test
    void parsesRealApiShape() throws Exception {
        LtaTrainAlerts alerts = new ObjectMapper().readValue(SAMPLE, LtaTrainAlerts.class);

        assertThat(alerts.value().status()).isEqualTo(2);
        assertThat(alerts.value().affectedSegments()).hasSize(1);
        assertThat(alerts.value().affectedSegments().get(0).line()).isEqualTo("EWL");
        assertThat(alerts.value().messages().get(0).content()).contains("No train service");
    }
}
