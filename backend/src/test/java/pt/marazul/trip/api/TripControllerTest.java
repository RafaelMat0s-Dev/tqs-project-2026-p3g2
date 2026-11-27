package pt.marazul.trip.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.marazul.booking.api.BookingReservationFacade;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.application.VehicleRepository;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.booking.infrastructure.InMemoryReservationRepository;
import pt.marazul.booking.infrastructure.InMemoryVehicleRepository;
import pt.marazul.booking.infrastructure.ContractMinimumBatteryAdapter;
import pt.marazul.support.FixedContractFacade;
import pt.marazul.trip.application.UnlockService;
import pt.marazul.trip.infrastructure.BookingReservationAdapter;
import pt.marazul.trip.infrastructure.ContractPickupWindowAdapter;
import pt.marazul.trip.infrastructure.InMemoryTripRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({UnlockService.class, BookingReservationAdapter.class, ContractPickupWindowAdapter.class, InMemoryTripRepository.class,
        BookingReservationFacade.class, ReservationService.class, InMemoryVehicleRepository.class,
        InMemoryReservationRepository.class, FixedContractFacade.class, ContractMinimumBatteryAdapter.class, TripControllerTest.FixedClock.class})
class TripControllerTest {
    static final Instant NOW = Instant.parse("2026-12-01T15:17:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    VehicleRepository vehicles;

    @Autowired
    ReservationService reservations;

    private final String marta = "marta-" + UUID.randomUUID();
    private final String pedro = "pedro-" + UUID.randomUUID();

    private UUID reservationFor(String guestId, String startsAt) {
        UUID vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 80, Vehicle.Status.AVAILABLE));
        Instant start = Instant.parse(startsAt);
        return reservations.reserve(vehicleId, guestId, start, start.plusSeconds(3600)).id();
    }

    private ResultActions unlock(Object reservationId, String guestId) throws Exception {
        return mvc.perform(post("/api/trips").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservationId\":\"" + reservationId + "\",\"guestId\":\"" + guestId + "\"}"));
    }

    @Test
    void unlockingInsideTheWindowStartsATrip() throws Exception {
        UUID id = reservationFor(marta, "2026-12-01T15:02:00Z");

        unlock(id, marta).andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(id.toString()))
                .andExpect(jsonPath("$.startedAt").value("2026-12-01T15:17:00Z"));
    }

    @Test
    void unlockingAfterTheWindowIsA422WithBr6() throws Exception {
        UUID id = reservationFor(marta, "2026-12-01T15:01:59Z");

        unlock(id, marta).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("BR6"));
    }

    @Test
    void anotherGuestsReservationIsForbidden() throws Exception {
        UUID id = reservationFor(pedro, "2026-12-01T15:02:00Z");

        unlock(id, marta).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Sem permissão"));
    }

    @Test
    void anUnknownReservationIsNotFound() throws Exception {
        unlock(UUID.randomUUID(), marta).andExpect(status().isNotFound());
    }

    @Test
    void aMalformedRequestIsA400() throws Exception {
        unlock("not-a-uuid", "").andExpect(status().isBadRequest());
    }
}
