package pt.marazul.fleet.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;

public interface VehicleRepository {
    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findById(UUID id);

    Optional<Vehicle> findByCode(String code);

    List<Vehicle> list(Optional<VehicleStatus> status, Optional<String> hub);

    /** Muda o estado só se ainda for {@code from}; devolve false se entretanto mudou. */
    boolean changeStatus(UUID id, VehicleStatus from, VehicleStatus to);

    boolean updateBatteryIfNewer(String code, int pct, Instant readingAt);
}
