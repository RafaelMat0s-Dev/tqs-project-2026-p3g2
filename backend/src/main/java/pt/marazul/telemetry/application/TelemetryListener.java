package pt.marazul.telemetry.application;

import pt.marazul.telemetry.domain.TelemetryReading;

public interface TelemetryListener {
    void onReading(TelemetryReading reading);

    void onInvalidMessage(String reason);
}
