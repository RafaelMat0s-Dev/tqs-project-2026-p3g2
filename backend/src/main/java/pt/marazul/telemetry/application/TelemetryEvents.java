package pt.marazul.telemetry.application;

import pt.marazul.telemetry.domain.TelemetryReading;

public interface TelemetryEvents {
    void readingApplied(TelemetryReading reading);
}
