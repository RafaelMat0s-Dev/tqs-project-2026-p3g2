package pt.marazul.booking.api;

import org.junit.jupiter.api.BeforeEach;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.booking.infrastructure.InMemoryReservationRepository;
import pt.marazul.booking.infrastructure.InMemoryVehicleRepository;
import pt.marazul.booking.infrastructure.ContractMinimumBatteryAdapter;
import pt.marazul.support.FixedContractFacade;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingReservationFacadeTest {
    private final InMemoryVehicleRepository vehicles = new InMemoryVehicleRepository();
    private final ReservationService service = new ReservationService(vehicles, new InMemoryReservationRepository(), new ContractMinimumBatteryAdapter(new FixedContractFacade()));
    private final ReservationFacade facade = new BookingReservationFacade(service);
    private final Instant start = Instant.parse("2099-01-01T15:02:00Z");
    private UUID vehicleId;
    private UUID reservationId;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 80, Vehicle.Status.AVAILABLE));
        reservationId = service.reserve(vehicleId, "marta", start, start.plusSeconds(3600)).id();
    }

    @Test
    void findsANewReservationAsReserved() {
        assertThat(facade.find(reservationId)).hasValueSatisfying(view -> {
            assertThat(view.guestId()).isEqualTo("marta");
            assertThat(view.vehicleId()).isEqualTo(vehicleId);
            assertThat(view.startsAt()).isEqualTo(start);
            assertThat(view.state()).isEqualTo(ReservationView.State.RESERVED);
        });
    }

    @Test
    void unknownReservationIsEmpty() {
        assertThat(facade.find(UUID.randomUUID())).isEmpty();
    }

    @Test
    void unlockingConvertsTheReservationAndPutsTheVehicleInUse() {
        facade.markUnlocked(reservationId);

        assertThat(facade.find(reservationId)).map(ReservationView::state).hasValue(ReservationView.State.CONVERTED);
        assertThat(vehicles.findById(vehicleId)).map(Vehicle::status).hasValue(Vehicle.Status.IN_USE);
    }

    @Test
    void expiringFreesTheVehicle() {
        facade.expire(reservationId);

        assertThat(facade.find(reservationId)).map(ReservationView::state).hasValue(ReservationView.State.EXPIRED);
        assertThat(vehicles.findById(vehicleId)).map(Vehicle::status).hasValue(Vehicle.Status.AVAILABLE);
    }

    @Test
    void anExpiredReservationNoLongerBlocksTheGuestOrTheVehicle() {
        facade.expire(reservationId);

        assertThat(service.reserve(vehicleId, "marta", start, start.plusSeconds(3600)).id()).isNotEqualTo(reservationId);
    }

    @Test
    void onlyReservedReservationsThatAlreadyStartedAreDueForExpiry() {
        assertThat(facade.findReservedStartedBefore(start)).isEmpty();
        assertThat(facade.findReservedStartedBefore(start.plusSeconds(1))).extracting(ReservationView::id).containsExactly(reservationId);

        facade.markUnlocked(reservationId);
        assertThat(facade.findReservedStartedBefore(start.plusSeconds(1))).isEmpty();
    }

    @Test
    void reservingPutsTheVehicleInReserved() {
        assertThat(vehicles.findById(vehicleId)).map(Vehicle::status).hasValue(Vehicle.Status.RESERVED);
    }

    enum Action { UNLOCK, EXPIRE }

    @ParameterizedTest(name = "de {0}, {1} é permitido: {2}")
    @CsvSource({
            "RESERVED, UNLOCK, true",
            "RESERVED, EXPIRE, true",
            "CONVERTED, UNLOCK, false",
            "CONVERTED, EXPIRE, false",
            "EXPIRED, UNLOCK, false",
            "EXPIRED, EXPIRE, false"
    })
    void onlyAReservedReservationCanBeUnlockedOrExpired(ReservationView.State from, Action action, boolean allowed) {
        switch (from) {
            case CONVERTED -> facade.markUnlocked(reservationId);
            case EXPIRED -> facade.expire(reservationId);
            case RESERVED -> { }
        }
        ThrowingCallable transition = action == Action.UNLOCK
                ? () -> facade.markUnlocked(reservationId)
                : () -> facade.expire(reservationId);

        if (allowed) {
            assertThatCode(transition).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(transition).isInstanceOf(IllegalStateException.class);
        }
    }
}
