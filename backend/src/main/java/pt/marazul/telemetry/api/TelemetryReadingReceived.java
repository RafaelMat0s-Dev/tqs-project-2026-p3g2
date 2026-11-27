package pt.marazul.telemetry.api;

import java.time.Instant;

public record TelemetryReadingReceived(
        String vehicleCode,
        Instant originAt,
        int batteryPercentage,
        boolean locked,
        String hubId,
        boolean shock) { }
