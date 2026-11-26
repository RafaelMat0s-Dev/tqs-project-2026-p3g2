package pt.marazul.telemetry.application;

import pt.marazul.telemetry.domain.TelemetryReading;

import java.util.Optional;

public interface VehicleTelemetryRepository {
    boolean saveIfNewer(TelemetryReading reading);

    Optional<TelemetryReading> findLatest(String vehicleCode);
}
