package pt.marazul.telemetry.application;

public interface TelemetrySource {
    void subscribe(TelemetryListener listener);
}
