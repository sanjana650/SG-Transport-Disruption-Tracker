package sg.tracker.model;

public enum Severity {
    /** Informational notice. */
    INFO,
    /** Delay but trains still running. */
    MINOR,
    /** No train service on the affected stretch. */
    MAJOR
}
