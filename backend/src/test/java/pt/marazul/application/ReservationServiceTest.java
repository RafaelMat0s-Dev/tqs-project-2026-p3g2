package pt.marazul.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.marazul.domain.ReservationRuleViolation;
import pt.marazul.domain.Vehicle;
import pt.marazul.infrastructure.InMemoryReservationRepository;
import pt.marazul.infrastructure.InMemoryVehicleRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class ReservationServiceTest {
    private final VehicleRepository vehicles = new InMemoryVehicleRepository();
    private final ReservationRepository reservations = new InMemoryReservationRepository();
    private ReservationService service;
    private UUID vehicleId;
    private final Instant start = Instant.parse("2099-01-01T10:00:00Z");
    private final Instant end = Instant.parse("2099-01-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        service = new ReservationService(vehicles, reservations);
        vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 25, Vehicle.Status.AVAILABLE));
    }

    @Test
    void reservesAvailableVehicleAtMinimumBattery() {
        assertThat(service.reserve(vehicleId, "guest-1", start, end).vehicleId()).isEqualTo(vehicleId);
    }

    @Test
    void rejectsBatteryBelowMinimumWithRuleId() {
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 24, Vehicle.Status.AVAILABLE));

        assertThatThrownBy(() -> service.reserve(vehicleId, "guest-1", start, end))
                .isInstanceOfSatisfying(ReservationRuleViolation.class, error -> assertThat(error.rule()).isEqualTo("BR1"));
    }

    @Test
    void rejectsOverlappingReservationsWithRuleId() {
        service.reserve(vehicleId, "guest-1", start, end);

        assertThatThrownBy(() -> service.reserve(vehicleId, "guest-2", start, end))
                .isInstanceOfSatisfying(ReservationRuleViolation.class, error -> assertThat(error.rule()).isEqualTo("BR7"));
    }
}
