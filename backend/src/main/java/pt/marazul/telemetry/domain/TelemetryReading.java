package pt.marazul.telemetry.domain;

import java.time.Instant;

public record TelemetryReading(
        String vehicleCode,
        Instant originAt,
        int batteryPercentage,
        boolean locked,
        String hubId,
        boolean shock) {

    public TelemetryReading {
        if (vehicleCode == null || vehicleCode.isBlank()) {
            throw new InvalidTelemetryReading("O código do veículo é obrigatório.");
        }
        if (originAt == null) {
            throw new InvalidTelemetryReading("O instante de origem da leitura é obrigatório.");
        }
        if (batteryPercentage < 0 || batteryPercentage > 100) {
            throw new InvalidTelemetryReading("A bateria tem de estar entre 0 e 100.");
        }
    }

    public boolean isNewerThan(TelemetryReading other) {
        return originAt.isAfter(other.originAt);
    }
}
