package pt.marazul.fleet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import pt.marazul.fleet.application.VehicleRepository;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;
import pt.marazul.support.TestcontainersConfiguration;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JpaVehicleRepositoryIT {
    private static final Instant T = Instant.parse("2026-11-20T14:31:07Z");

    @Autowired VehicleRepository repository;
    @Autowired FleetVehicleJpaRepository jpa;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate tx;

    @BeforeEach
    void cleanTable() {
        jdbc.update("delete from fleet_vehicle");
    }

    private Vehicle registered(String code, String hub) {
        return repository.save(Vehicle.register(UUID.randomUUID(), code, "BICYCLE", hub));
    }

    @Test
    void savedVehicleIsFoundByIdAndByCode() {
        Vehicle saved = registered("B-17", "sao-bras");

        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findByCode("B-17")).contains(saved);
        assertThat(repository.findByCode("B-99")).isEmpty();
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void newVehicleHasUnknownBatteryBeforeFirstReading() {
        Vehicle saved = registered("B-17", "sao-bras");

        Vehicle found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.batteryPercentage()).isNull();
        assertThat(found.lastReadingAt()).isNull();
        assertThat(found.status()).isEqualTo(VehicleStatus.DISABLED);
    }

    @Test
    void savingExistingVehicleUpdatesItsStatus() {
        Vehicle saved = registered("B-17", "sao-bras");

        repository.save(saved.moveTo(VehicleStatus.AVAILABLE));

        assertThat(repository.findById(saved.id()).orElseThrow().status())
                .isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void secondVehicleWithSameCodeFails() {
        registered("B-17", "sao-bras");

        assertThatThrownBy(() -> registered("B-17", "praia-da-ribeira"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void staleConcurrentWriteOfTheSameRowFailsWithOptimisticLock() {
        Vehicle saved = registered("B-17", "sao-bras");
        var first = tx.execute(s -> jpa.findById(saved.id()).orElseThrow());
        var second = tx.execute(s -> jpa.findById(saved.id()).orElseThrow());
        ReflectionTestUtils.setField(first, "baseHubCode", "praia-da-ribeira");
        ReflectionTestUtils.setField(second, "baseHubCode", "hotel-alvina-mar");

        jpa.saveAndFlush(first);

        assertThatThrownBy(() -> jpa.saveAndFlush(second))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void updateBatteryIfNewerStoresFirstReading() {
        Vehicle saved = registered("B-17", "sao-bras");

        assertThat(repository.updateBatteryIfNewer("B-17", 47, T)).isTrue();

        Vehicle found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.batteryPercentage()).isEqualTo(47);
        assertThat(found.lastReadingAt()).isEqualTo(T);
    }

    @Test
    void updateBatteryIfNewerReplacesWithMoreRecentReading() {
        Vehicle saved = registered("B-17", "sao-bras");
        repository.updateBatteryIfNewer("B-17", 47, T);

        assertThat(repository.updateBatteryIfNewer("B-17", 45, T.plusSeconds(2))).isTrue();

        assertThat(repository.findById(saved.id()).orElseThrow().batteryPercentage()).isEqualTo(45);
    }

    @Test
    void updateBatteryIfNewerIgnoresOlderReading() {
        Vehicle saved = registered("B-17", "sao-bras");
        repository.updateBatteryIfNewer("B-17", 47, T);

        assertThat(repository.updateBatteryIfNewer("B-17", 30, T.minusSeconds(5))).isFalse();

        Vehicle found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.batteryPercentage()).isEqualTo(47);
        assertThat(found.lastReadingAt()).isEqualTo(T);
    }

    @Test
    void updateBatteryIfNewerIgnoresEqualReading() {
        Vehicle saved = registered("B-17", "sao-bras");
        repository.updateBatteryIfNewer("B-17", 47, T);

        assertThat(repository.updateBatteryIfNewer("B-17", 30, T)).isFalse();

        assertThat(repository.findById(saved.id()).orElseThrow().batteryPercentage()).isEqualTo(47);
    }

    @Test
    void updateBatteryIfNewerForUnknownCodeReturnsFalse() {
        assertThat(repository.updateBatteryIfNewer("B-99", 47, T)).isFalse();
    }

    @Test
    void updateBatteryDoesNotChangeStatus() {
        Vehicle saved = registered("B-17", "sao-bras");
        repository.save(saved.moveTo(VehicleStatus.AVAILABLE));

        repository.updateBatteryIfNewer("B-17", 47, T);

        assertThat(repository.findById(saved.id()).orElseThrow().status())
                .isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void listFiltersByStatusAndHub() {
        Vehicle b17 = registered("B-17", "sao-bras");
        registered("B-18", "praia-da-ribeira");
        Vehicle b19 = registered("B-19", "sao-bras");
        repository.save(b17.moveTo(VehicleStatus.AVAILABLE));

        assertThat(repository.list(Optional.empty(), Optional.empty())).hasSize(3);
        assertThat(repository.list(Optional.of(VehicleStatus.AVAILABLE), Optional.empty()))
                .extracting(Vehicle::code).containsExactly("B-17");
        assertThat(repository.list(Optional.empty(), Optional.of("sao-bras")))
                .extracting(Vehicle::code).containsExactlyInAnyOrder("B-17", "B-19");
        assertThat(repository.list(Optional.of(VehicleStatus.DISABLED), Optional.of("sao-bras")))
                .extracting(Vehicle::id).containsExactly(b19.id());
        assertThat(repository.list(Optional.of(VehicleStatus.RETIRED), Optional.empty())).isEmpty();
    }
}
