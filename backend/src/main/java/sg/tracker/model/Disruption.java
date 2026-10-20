package sg.tracker.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "disruptions", indexes = @Index(columnList = "fingerprint"))
public class Disruption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** line|direction|stations - identifies "the same disruption" across polls. */
    @Column(nullable = false)
    private String fingerprint;

    @Column(nullable = false)
    private String line;

    private String direction;

    @Column(length = 500)
    private String stations;

    @Column(length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    private boolean freeBusAvailable;

    private boolean active;

    @Column(nullable = false)
    private Instant firstSeen;

    @Column(nullable = false)
    private Instant lastSeen;

    private Instant resolvedAt;

    protected Disruption() {
    }

    public Disruption(String fingerprint, String line, String direction, String stations,
                      String message, Severity severity, boolean freeBusAvailable, Instant now) {
        this.fingerprint = fingerprint;
        this.line = line;
        this.direction = direction;
        this.stations = stations;
        this.message = message;
        this.severity = severity;
        this.freeBusAvailable = freeBusAvailable;
        this.active = true;
        this.firstSeen = now;
        this.lastSeen = now;
    }

    /** Called when the same disruption is seen again on a later poll. */
    public void refresh(String message, Severity severity, boolean freeBusAvailable, Instant now) {
        this.message = message;
        this.severity = severity;
        this.freeBusAvailable = freeBusAvailable;
        this.active = true;
        this.resolvedAt = null;
        this.lastSeen = now;
    }

    public void resolve(Instant now) {
        this.active = false;
        this.resolvedAt = now;
    }

    public Long getId() { return id; }
    public String getFingerprint() { return fingerprint; }
    public String getLine() { return line; }
    public String getDirection() { return direction; }
    public String getStations() { return stations; }
    public String getMessage() { return message; }
    public Severity getSeverity() { return severity; }
    public boolean isFreeBusAvailable() { return freeBusAvailable; }
    public boolean isActive() { return active; }
    public Instant getFirstSeen() { return firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public Instant getResolvedAt() { return resolvedAt; }
}
