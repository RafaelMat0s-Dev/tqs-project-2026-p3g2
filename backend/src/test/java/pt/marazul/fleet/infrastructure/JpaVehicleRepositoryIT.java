package pt.marazul.fleet.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import pt.marazul.shared.domain.BusinessRuleConflict;
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
    void savingExistingVehicleDoesNotChangeItsStatus() {
        Vehicle saved = registered("B-17", "sao-bras");

        repository.save(saved.moveTo(VehicleStatus.AVAILABLE));

        assertThat(repository.findById(saved.id()).orElseThrow().status())
                .isEqualTo(VehicleStatus.DISABLED);
    }

    @Test
    void secondVehicleWithSameCodeIsConflictWithRuleVehicleCode() {
        registered("B-17", "sao-bras");

        assertThatThrownBy(() -> registered("B-17", "praia-da-ribeira"))
                .isInstanceOfSatisfying(BusinessRuleConflict.class,
                        e -> assertThat(e.rule()).isEqualTo("VEHICLE_CODE"));
    }

    @Test
    void changeStatusWithTheRightSourceChangesStatusAndVersion() {
        Vehicle saved = registered("B-17", "sao-bras");
        long before = versionOf(saved.id());

        assertThat(repository.changeStatus(saved.id(), VehicleStatus.DISABLED, VehicleStatus.AVAILABLE)).isTrue();

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(versionOf(saved.id())).isEqualTo(before + 1);
    }

    @Test
    void changeStatusWithTheWrongSourceReturnsFalseAndChangesNothing() {
        Vehicle saved = registered("B-17", "sao-bras");
        long before = versionOf(saved.id());

        assertThat(repository.changeStatus(saved.id(), VehicleStatus.AVAILABLE, VehicleStatus.RESERVED)).isFalse();

        assertThat(repository.findById(saved.id()).orElseThrow().status()).isEqualTo(VehicleStatus.DISABLED);
        assertThat(versionOf(saved.id())).isEqualTo(before);
    }

    @Test
    void changeStatusAfterANewerReadingKeepsTheNewerBatteryAndReading() {
        Vehicle saved = registered("B-17", "sao-bras");
        Vehicle readEarlier = repository.findById(saved.id()).orElseThrow();
        repository.updateBatteryIfNewer("B-17", 40, T);

        repository.changeStatus(readEarlier.id(), readEarlier.status(), VehicleStatus.AVAILABLE);

        Vehicle found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(found.batteryPercentage()).isEqualTo(40);
        assertThat(found.lastReadingAt()).isEqualTo(T);
    }

    @Test
    void savingAnExistingVehicleRewritesNeitherBatteryNorReading() {
        Vehicle saved = registered("B-17", "sao-bras");
        repository.updateBatteryIfNewer("B-17", 40, T);

        repository.save(saved);

        Vehicle found = repository.findById(saved.id()).orElseThrow();
        assertThat(found.batteryPercentage()).isEqualTo(40);
        assertThat(found.lastReadingAt()).isEqualTo(T);
    }

    @Test
    void concurrentRegistrationsOfTheSameCodeYieldExactlyOneSuccessAndOneConflict() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 10; i++) {
                String code = "B-" + (20 + i);
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Object>> results = new ArrayList<>();
                for (int t = 0; t < 2; t++) {
                    results.add(pool.submit(() -> {
                        start.await();
                        try {
                            return registered(code, "sao-bras");
                        } catch (Exception e) {
                            return e;
                        }
                    }));
                }
                start.countDown();
                List<Object> outcomes = new ArrayList<>();
                for (Future<Object> f : results) {
                    outcomes.add(f.get(10, TimeUnit.SECONDS));
                }
                assertThat(outcomes).filteredOn(o -> o instanceof Vehicle).hasSize(1);
                assertThat(outcomes).filteredOn(o -> o instanceof Exception)
                        .hasSize(1).allSatisfy(o -> assertThat(o).isInstanceOf(BusinessRuleConflict.class));
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentChangeStatusFromTheSameStateSucceedsExactlyOnce() throws Exception {
        Vehicle saved = registered("B-17", "sao-bras");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (VehicleStatus target : List.of(VehicleStatus.AVAILABLE, VehicleStatus.RETIRED)) {
                results.add(pool.submit(() -> {
                    start.await();
                    return repository.changeStatus(saved.id(), VehicleStatus.DISABLED, target);
                }));
            }
            start.countDown();
            int successes = 0;
            for (Future<Boolean> f : results) {
                successes += f.get(10, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertThat(successes).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"x-17", "B-1", "B-1000", "b-17", "B-17x"})
    void databaseRejectsCodesOutsideThePattern(String code) {
        assertThatThrownBy(() -> jdbc.update(
                "insert into fleet_vehicle (id, code, type, base_hub_code, status, version, created_at)"
                        + " values (?, ?, 'BICYCLE', 'sao-bras', 'DISABLED', 0, now())",
                UUID.randomUUID(), code))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long versionOf(UUID id) {
        return jdbc.queryForObject("select version from fleet_vehicle where id = ?", Long.class, id);
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
        repository.changeStatus(saved.id(), VehicleStatus.DISABLED, VehicleStatus.AVAILABLE);

        repository.updateBatteryIfNewer("B-17", 47, T);

        assertThat(repository.findById(saved.id()).orElseThrow().status())
                .isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void listFiltersByStatusAndHub() {
        Vehicle b17 = registered("B-17", "sao-bras");
        registered("B-18", "praia-da-ribeira");
        Vehicle b19 = registered("B-19", "sao-bras");
        repository.changeStatus(b17.id(), VehicleStatus.DISABLED, VehicleStatus.AVAILABLE);

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
