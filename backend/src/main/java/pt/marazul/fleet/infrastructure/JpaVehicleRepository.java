package pt.marazul.fleet.infrastructure;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import pt.marazul.fleet.application.VehicleRepository;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;

@Repository
public class JpaVehicleRepository implements VehicleRepository {
    private final FleetVehicleJpaRepository jpa;
    private final Clock clock;

    public JpaVehicleRepository(FleetVehicleJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        FleetVehicleEntity entity = jpa.findById(vehicle.id())
                .orElseGet(() -> new FleetVehicleEntity(vehicle.id(), clock.instant()));
        entity.copyFrom(vehicle);
        return jpa.saveAndFlush(entity).toDomain();
    }

    @Override
    public boolean changeStatus(UUID id, VehicleStatus from, VehicleStatus to) {
        throw new UnsupportedOperationException("esqueleto SCRUM-20");
    }

    @Override
    public Optional<Vehicle> findById(UUID id) {
        return jpa.findById(id).map(FleetVehicleEntity::toDomain);
    }

    @Override
    public Optional<Vehicle> findByCode(String code) {
        return jpa.findByCode(code).map(FleetVehicleEntity::toDomain);
    }

    @Override
    public List<Vehicle> list(Optional<VehicleStatus> status, Optional<String> hub) {
        return jpa.search(status.orElse(null), hub.orElse(null)).stream()
                .map(FleetVehicleEntity::toDomain).toList();
    }

    @Override
    public boolean updateBatteryIfNewer(String code, int pct, Instant readingAt) {
        return jpa.updateBatteryIfNewer(code, pct, readingAt) > 0;
    }
}
