package pt.marazul.fleet.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;

/** Falso em memória do porto, partilhado pelos testes unitários da fleet. */
public class InMemoryVehicles implements VehicleRepository {
    private final Map<UUID, Vehicle> store = new LinkedHashMap<>();

    @Override
    public Vehicle save(Vehicle vehicle) {
        store.put(vehicle.id(), vehicle);
        return vehicle;
    }

    /** Quando verdadeiro simula um estado alterado entretanto. */
    public boolean changeStatusConflicts;
    /** Pedidos feitos a changeStatus, como "DE>PARA". */
    public final List<String> changes = new java.util.ArrayList<>();

    @Override
    public boolean changeStatus(UUID id, VehicleStatus from, VehicleStatus to) {
        changes.add(from + ">" + to);
        Vehicle v = store.get(id);
        if (changeStatusConflicts || v == null || v.status() != from) {
            return false;
        }
        store.put(id, v.moveTo(to));
        return true;
    }

    @Override
    public Optional<Vehicle> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Vehicle> findByCode(String code) {
        return store.values().stream().filter(v -> v.code().equals(code)).findFirst();
    }

    @Override
    public List<Vehicle> list(Optional<VehicleStatus> status, Optional<String> hub) {
        return store.values().stream()
                .filter(v -> status.map(s -> v.status() == s).orElse(true))
                .filter(v -> hub.map(h -> v.baseHubCode().equals(h)).orElse(true))
                .toList();
    }

    @Override
    public boolean updateBatteryIfNewer(String code, int pct, Instant readingAt) {
        return findByCode(code).map(v -> {
            Vehicle updated = v.withBattery(pct, readingAt);
            store.put(v.id(), updated);
            return updated != v;
        }).orElse(false);
    }

    /** Semeia um veículo diretamente num estado, sem passar pelas transições. */
    public Vehicle seed(String code, String hub, VehicleStatus status) {
        Vehicle v = Vehicle.register(UUID.randomUUID(), code, "BICYCLE", hub);
        return save(new Vehicle(v.id(), v.code(), v.type(), v.baseHubCode(), status, null, null));
    }
}
