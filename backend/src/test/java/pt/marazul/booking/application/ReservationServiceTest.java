package pt.marazul.booking.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.marazul.booking.domain.ReservationRuleViolation;
import pt.marazul.shared.domain.BusinessRuleConflict;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.booking.infrastructure.InMemoryReservationRepository;
import pt.marazul.booking.infrastructure.InMemoryVehicleRepository;
import pt.marazul.booking.infrastructure.ContractMinimumBatteryAdapter;
import pt.marazul.support.FixedContractFacade;

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
        service = new ReservationService(vehicles, reservations, new ContractMinimumBatteryAdapter(new FixedContractFacade()));
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

    @ParameterizedTest(name = "mínimo {0} % do contrato, bateria {1} %: aceite = {2}")
    @CsvSource({"40, 39, false", "40, 40, true", "0, 0, true", "100, 99, false", "100, 100, true"})
    void minimumBatteryComesFromTheContract(int minimum, int battery, boolean accepted) {
        ReservationService strict = new ReservationService(vehicles, reservations,
                new ContractMinimumBatteryAdapter(new FixedContractFacade(minimum)));
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", battery, Vehicle.Status.AVAILABLE));

        if (accepted) {
            assertThat(strict.reserve(vehicleId, "guest-1", start, end).vehicleId()).isEqualTo(vehicleId);
        } else {
            assertThatThrownBy(() -> strict.reserve(vehicleId, "guest-1", start, end))
                    .isInstanceOfSatisfying(ReservationRuleViolation.class, error -> assertThat(error.rule()).isEqualTo("BR1"));
        }
    }

    @Test
    void rejectsOverlappingReservationsWithRuleId() {
        service.reserve(vehicleId, "guest-1", start, end);

        assertThatThrownBy(() -> service.reserve(vehicleId, "guest-2", start, end))
                .isInstanceOfSatisfying(BusinessRuleConflict.class, error -> assertThat(error.rule()).isEqualTo("BR7"));
    }
}
