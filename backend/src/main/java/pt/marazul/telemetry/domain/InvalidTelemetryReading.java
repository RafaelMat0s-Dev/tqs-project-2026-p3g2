package pt.marazul.telemetry.domain;

public class InvalidTelemetryReading extends RuntimeException {
    public InvalidTelemetryReading(String message) {
        super(message);
    }
}
