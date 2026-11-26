package pt.marazul.telemetry.application;

import java.time.Duration;

public interface TelemetryMetrics {
    void readingApplied(Duration delay);

    void readingDiscarded();

    void readingRejected();
}
