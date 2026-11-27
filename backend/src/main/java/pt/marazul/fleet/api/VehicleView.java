package pt.marazul.fleet.api;

import java.time.Instant;
import java.util.UUID;
import pt.marazul.fleet.domain.Vehicle;

public record VehicleView(UUID id, String code, String type, String baseHubCode, Status status,
        Integer batteryPercentage, Instant lastReadingAt) {

    public enum Status { DISABLED, AVAILABLE, RESERVED, IN_USE, MAINTENANCE, RETIRED }

    static VehicleView of(Vehicle v) {
        return new VehicleView(v.id(), v.code(), v.type(), v.baseHubCode(),
                Status.valueOf(v.status().name()), v.batteryPercentage(), v.lastReadingAt());
    }
}
