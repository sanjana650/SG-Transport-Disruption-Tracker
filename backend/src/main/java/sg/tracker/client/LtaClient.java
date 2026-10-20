package sg.tracker.client;

/** Source of train alerts. Real implementation calls LTA DataMall; mock returns sample data. */
public interface LtaClient {
    LtaTrainAlerts fetchTrainAlerts();
}
