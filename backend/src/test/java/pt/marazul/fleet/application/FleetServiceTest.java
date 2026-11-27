package pt.marazul.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import pt.marazul.fleet.domain.Vehicle;
import pt.marazul.fleet.domain.VehicleStatus;
import pt.marazul.shared.domain.BusinessRuleConflict;
import pt.marazul.shared.domain.BusinessRuleViolation;

class FleetServiceTest {
    private static final Instant T = Instant.parse("2026-11-20T14:31:07Z");

    private InMemoryVehicles vehicles;
    private FleetService service;

    @BeforeEach
    void setUp() {
        vehicles = new InMemoryVehicles();
        service = new FleetService(vehicles);
    }

    @Test
    void registerCreatesDisabledVehicle() {
        Vehicle v = service.register("B-17", "BICYCLE", "sao-bras");

        assertThat(v.status()).isEqualTo(VehicleStatus.DISABLED);
        assertThat(v.code()).isEqualTo("B-17");
        assertThat(v.baseHubCode()).isEqualTo("sao-bras");
        assertThat(v.id()).isNotNull();
        assertThat(vehicles.findById(v.id())).contains(v);
    }

    @Test
    void registerDuplicateCodeIsConflictWithRuleVehicleCode() {
        service.register("B-17", "BICYCLE", "sao-bras");

        assertThatThrownBy(() -> service.register("B-17", "BICYCLE", "praia-da-ribeira"))
                .isInstanceOf(BusinessRuleConflict.class)
                .extracting(e -> ((BusinessRuleConflict) e).rule()).isEqualTo("VEHICLE_CODE");
    }

    record Op(String name, BiFunction<FleetService, UUID, Vehicle> call, VehicleStatus target) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Op> operations() {
        return Stream.of(
                new Op("activate", FleetService::activate, VehicleStatus.AVAILABLE),
                new Op("deactivate", FleetService::deactivate, VehicleStatus.DISABLED),
                new Op("retire", FleetService::retire, VehicleStatus.RETIRED),
                new Op("markReserved", FleetService::markReserved, VehicleStatus.RESERVED),
                new Op("release", FleetService::release, VehicleStatus.AVAILABLE),
                new Op("startTrip", FleetService::startTrip, VehicleStatus.IN_USE),
                new Op("endTrip", FleetService::endTrip, VehicleStatus.AVAILABLE),
                new Op("sendToMaintenance", FleetService::sendToMaintenance, VehicleStatus.MAINTENANCE),
                new Op("completeMaintenance", FleetService::completeMaintenance, VehicleStatus.AVAILABLE));
    }

    static Stream<Arguments> operationsByInitialState() {
        return operations().flatMap(op -> Stream.of(VehicleStatus.values())
                .map(from -> Arguments.of(op, from)));
    }

    @ParameterizedTest(name = "{0} a partir de {1}")
    @MethodSource("operationsByInitialState")
    void operationAppliesTransitionOrFailsWithLifecycleRule(Op op, VehicleStatus from) {
        Vehicle seeded = vehicles.seed("B-17", "sao-bras", from);

        if (from.canMoveTo(op.target())) {
            Vehicle result = op.call().apply(service, seeded.id());

            assertThat(result.status()).isEqualTo(op.target());
            assertThat(vehicles.findById(seeded.id()).orElseThrow().status()).isEqualTo(op.target());
        } else {
            assertThatThrownBy(() -> op.call().apply(service, seeded.id()))
                    .isInstanceOfSatisfying(BusinessRuleViolation.class,
                            e -> assertThat(e.rule()).isEqualTo("VEHICLE_LIFECYCLE"));
            assertThat(vehicles.findById(seeded.id()).orElseThrow().status()).isEqualTo(from);
        }
    }

    static Stream<Arguments> operationsWithAValidSourceState() {
        return operationsByInitialState()
                .filter(a -> ((VehicleStatus) a.get()[1]).canMoveTo(((Op) a.get()[0]).target()));
    }

    @ParameterizedTest(name = "{0} a partir de {1} chama changeStatus(lido, alvo)")
    @MethodSource("operationsWithAValidSourceState")
    void transitionCallsChangeStatusWithTheReadStateAndTheTarget(Op op, VehicleStatus from) {
        Vehicle seeded = vehicles.seed("B-17", "sao-bras", from);

        op.call().apply(service, seeded.id());

        assertThat(vehicles.changes).containsExactly(from + ">" + op.target());
    }

    @ParameterizedTest(name = "{0} a partir de {1} com estado alterado entretanto")
    @MethodSource("operationsWithAValidSourceState")
    void stateChangedMeanwhileIsConflictWithRuleVehicleStateChanged(Op op, VehicleStatus from) {
        Vehicle seeded = vehicles.seed("B-17", "sao-bras", from);
        vehicles.changeStatusConflicts = true;

        assertThatThrownBy(() -> op.call().apply(service, seeded.id()))
                .isInstanceOfSatisfying(BusinessRuleConflict.class, e -> {
                    assertThat(e.rule()).isEqualTo("VEHICLE_STATE_CHANGED");
                    assertThat(e.getMessage())
                            .isEqualTo("O estado do veículo B-17 mudou entretanto; repita a operação.");
                });
        assertThat(vehicles.findById(seeded.id()).orElseThrow().status()).isEqualTo(from);
    }

    @ParameterizedTest(name = "{0} com id inexistente")
    @MethodSource("operations")
    void operationOnUnknownIdIsVehicleNotFound(Op op) {
        assertThatThrownBy(() -> op.call().apply(service, UUID.randomUUID()))
                .isInstanceOf(VehicleNotFound.class);
    }

    @Test
    void findByIdAndFindByCodeReturnRegisteredVehicle() {
        Vehicle v = service.register("B-17", "BICYCLE", "sao-bras");

        assertThat(service.findById(v.id())).contains(v);
        assertThat(service.findByCode("B-17")).contains(v);
        assertThat(service.findByCode("B-99")).isEmpty();
    }

    @Test
    void applyReadingUpdatesBatteryWhenNewer() {
        Vehicle v = service.register("B-17", "BICYCLE", "sao-bras");

        service.applyReading("B-17", 47, T);

        Vehicle after = vehicles.findById(v.id()).orElseThrow();
        assertThat(after.batteryPercentage()).isEqualTo(47);
        assertThat(after.lastReadingAt()).isEqualTo(T);
        assertThat(after.status()).isEqualTo(VehicleStatus.DISABLED);
    }

    @ParameterizedTest(name = "leitura {0}s depois da atual não altera a bateria")
    @org.junit.jupiter.params.provider.ValueSource(longs = {-5, 0})
    void applyReadingIgnoresOlderOrEqualReading(long offsetSeconds) {
        Vehicle v = service.register("B-17", "BICYCLE", "sao-bras");
        service.applyReading("B-17", 47, T);

        service.applyReading("B-17", 30, T.plusSeconds(offsetSeconds));

        assertThat(vehicles.findById(v.id()).orElseThrow().batteryPercentage()).isEqualTo(47);
    }

    @Test
    void applyReadingForUnknownCodeIsIgnoredWithoutError() {
        service.applyReading("B-99", 47, T);

        assertThat(vehicles.findByCode("B-99")).isEmpty();
    }

    @Test
    void listFiltersByStatusAndByHub() {
        vehicles.seed("B-17", "sao-bras", VehicleStatus.AVAILABLE);
        vehicles.seed("B-18", "praia-da-ribeira", VehicleStatus.AVAILABLE);
        vehicles.seed("B-19", "sao-bras", VehicleStatus.MAINTENANCE);

        assertThat(service.list(Optional.empty(), Optional.empty())).hasSize(3);
        assertThat(service.list(Optional.of(VehicleStatus.AVAILABLE), Optional.empty()))
                .extracting(Vehicle::code).containsExactlyInAnyOrder("B-17", "B-18");
        assertThat(service.list(Optional.empty(), Optional.of("sao-bras")))
                .extracting(Vehicle::code).containsExactlyInAnyOrder("B-17", "B-19");
        assertThat(service.list(Optional.of(VehicleStatus.AVAILABLE), Optional.of("sao-bras")))
                .extracting(Vehicle::code).containsExactly("B-17");
    }

    @ParameterizedTest
    @EnumSource(VehicleStatus.class)
    void listByStatusReturnsOnlyThatStatus(VehicleStatus status) {
        for (VehicleStatus s : VehicleStatus.values()) {
            vehicles.seed("B-" + (10 + s.ordinal()), "sao-bras", s);
        }

        assertThat(service.list(Optional.of(status), Optional.empty()))
                .extracting(Vehicle::status).containsOnly(status).hasSize(1);
    }
}
