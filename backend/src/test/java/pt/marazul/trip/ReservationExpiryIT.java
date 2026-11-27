package pt.marazul.trip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.marazul.booking.api.ReservationFacade;
import pt.marazul.booking.api.ReservationView;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.application.VehicleRepository;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.support.TestcontainersConfiguration;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "marazul.trip.expiry-interval=100ms")
@Import(TestcontainersConfiguration.class)
class ReservationExpiryIT {
    @Autowired
    ReservationService reservations;

    @Autowired
    ReservationFacade facade;

    @Autowired
    VehicleRepository vehicles;

    @Autowired
    Clock clock;

    @Test
    void theScheduledTaskExpiresAReservationThatIsNotUnlockedAndFreesTheVehicle() {
        UUID vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 80, Vehicle.Status.AVAILABLE));
        Instant start = clock.instant().minus(Duration.ofMinutes(16));
        UUID reservationId = reservations.reserve(vehicleId, "marta-" + UUID.randomUUID(), start, start.plusSeconds(3600)).id();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(facade.find(reservationId)).map(ReservationView::state).hasValue(ReservationView.State.EXPIRED);
            assertThat(vehicles.findById(vehicleId)).map(Vehicle::status).hasValue(Vehicle.Status.AVAILABLE);
        });
    }
}
