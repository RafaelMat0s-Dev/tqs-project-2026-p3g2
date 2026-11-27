package pt.marazul.booking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.api.VehicleView;
import pt.marazul.shared.domain.BusinessRuleViolation;

class FleetVehicleRepositoryTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000017");

    private FleetApi fleet;
    private FleetVehicleRepository repository;

    @BeforeEach
    void setUp() {
        fleet = mock(FleetApi.class);
        repository = new FleetVehicleRepository(fleet);
    }

    private static VehicleView view(VehicleView.Status status, Integer battery) {
        return new VehicleView(ID, "B-17", "E_BIKE", "sao-bras", status, battery,
                battery == null ? null : Instant.parse("2026-11-20T14:31:07Z"));
    }

    @Test
    void findByIdMapsFieldsAndUsesTheBaseHubAsHub() {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(VehicleView.Status.AVAILABLE, 47)));

        assertThat(repository.findById(ID))
                .contains(new Vehicle(ID, "sao-bras", "E_BIKE", 47, Vehicle.Status.AVAILABLE));
    }

    @Test
    void unknownBatteryMapsToZeroSoTheRuleRefusesIt() {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(VehicleView.Status.AVAILABLE, null)));

        assertThat(repository.findById(ID)).map(Vehicle::batteryPercentage).contains(0);
    }

    @ParameterizedTest
    @EnumSource(VehicleView.Status.class)
    void everyStatusIsMappedByName(VehicleView.Status status) {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(status, 80)));

        assertThat(repository.findById(ID)).map(v -> v.status().name()).contains(status.name());
    }

    @Test
    void findByIdIsEmptyWhenTheFleetDoesNotKnowTheVehicle() {
        when(fleet.findById(ID)).thenReturn(Optional.empty());

        assertThat(repository.findById(ID)).isEmpty();
    }

    @Test
    void findAllMapsEveryVehicleOfTheFleet() {
        when(fleet.list(Optional.empty(), Optional.empty()))
                .thenReturn(List.of(view(VehicleView.Status.AVAILABLE, 80), view(VehicleView.Status.RESERVED, null)));

        assertThat(repository.findAll()).extracting(Vehicle::status, Vehicle::batteryPercentage)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(Vehicle.Status.AVAILABLE, 80),
                        org.assertj.core.groups.Tuple.tuple(Vehicle.Status.RESERVED, 0));
    }

    record Transition(VehicleView.Status from, Vehicle.Status to, BiConsumer<FleetApi, UUID> expected) { }

    static List<Transition> validTransitions() {
        List<Transition> all = new java.util.ArrayList<>(List.of(
                new Transition(VehicleView.Status.AVAILABLE, Vehicle.Status.RESERVED, FleetApi::markReserved),
                new Transition(VehicleView.Status.RESERVED, Vehicle.Status.AVAILABLE, FleetApi::release),
                new Transition(VehicleView.Status.RESERVED, Vehicle.Status.IN_USE, FleetApi::startTrip),
                new Transition(VehicleView.Status.IN_USE, Vehicle.Status.AVAILABLE, FleetApi::endTrip)));
        for (VehicleView.Status from : VehicleView.Status.values()) {
            if (from != VehicleView.Status.MAINTENANCE) {
                all.add(new Transition(from, Vehicle.Status.MAINTENANCE, FleetApi::sendToMaintenance));
            }
        }
        return all;
    }

    @ParameterizedTest
    @MethodSource("validTransitions")
    void saveCallsTheMatchingFleetOperation(Transition t) {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(t.from(), 80)));

        repository.save(new Vehicle(ID, "sao-bras", "E_BIKE", 80, t.to()));

        t.expected().accept(verify(fleet), ID);
    }

    @ParameterizedTest
    @EnumSource(value = VehicleView.Status.class, mode = EnumSource.Mode.EXCLUDE, names = "RESERVED")
    void saveWithTheSameStateCallsNoOperation(VehicleView.Status status) {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(status, 80)));

        repository.save(new Vehicle(ID, "sao-bras", "E_BIKE", 80, Vehicle.Status.valueOf(status.name())));

        verify(fleet, never()).markReserved(ID);
        verify(fleet, never()).release(ID);
        verify(fleet, never()).startTrip(ID);
        verify(fleet, never()).endTrip(ID);
        verify(fleet, never()).sendToMaintenance(ID);
    }

    @Test
    void saveReservedWhileTheFleetIsAlreadyReservedStillCallsMarkReservedOnceAndPropagatesItsRefusal() {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(VehicleView.Status.RESERVED, 80)));
        when(fleet.markReserved(ID)).thenThrow(new BusinessRuleViolation("VEHICLE_LIFECYCLE", "Transição inválida"));

        assertThatThrownBy(() -> repository.save(new Vehicle(ID, "sao-bras", "E_BIKE", 80, Vehicle.Status.RESERVED)))
                .isInstanceOf(BusinessRuleViolation.class);
        verify(fleet, times(1)).markReserved(ID);
    }

    @ParameterizedTest
    @MethodSource("invalidPairs")
    void saveOfAnyOtherPairThrowsIllegalStateAndChangesNothing(VehicleView.Status from, Vehicle.Status to) {
        when(fleet.findById(ID)).thenReturn(Optional.of(view(from, 80)));

        assertThatThrownBy(() -> repository.save(new Vehicle(ID, "sao-bras", "E_BIKE", 80, to)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(to.name());
        verify(fleet, never()).markReserved(ID);
        verify(fleet, never()).release(ID);
        verify(fleet, never()).startTrip(ID);
        verify(fleet, never()).endTrip(ID);
        verify(fleet, never()).sendToMaintenance(ID);
    }

    static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidPairs() {
        List<Transition> valid = validTransitions();
        return java.util.Arrays.stream(VehicleView.Status.values())
                .flatMap(from -> java.util.Arrays.stream(Vehicle.Status.values()).map(to -> new Object[] {from, to}))
                .filter(p -> !p[0].toString().equals(p[1].toString()))
                .filter(p -> valid.stream().noneMatch(t -> t.from() == p[0] && t.to() == p[1]))
                .map(p -> org.junit.jupiter.params.provider.Arguments.of(p[0], p[1]));
    }

    @Test
    void saveOfAnUnknownVehicleThrowsIllegalStateWithoutCallingAnOperation() {
        when(fleet.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.save(new Vehicle(ID, "sao-bras", "E_BIKE", 80, Vehicle.Status.RESERVED)))
                .isInstanceOf(IllegalStateException.class);
        verify(fleet, never()).markReserved(ID);
    }
}
