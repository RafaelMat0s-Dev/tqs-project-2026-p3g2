package pt.marazul.booking.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.application.VehicleRepository;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.booking.infrastructure.InMemoryReservationRepository;
import pt.marazul.booking.infrastructure.InMemoryVehicleRepository;
import pt.marazul.booking.infrastructure.ContractMinimumBatteryAdapter;
import pt.marazul.support.FixedContractFacade;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ReservationService.class, InMemoryVehicleRepository.class, InMemoryReservationRepository.class, FixedContractFacade.class,
        ContractMinimumBatteryAdapter.class})
class ReservationControllerTest {
    private static final Instant START = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
    private static final Instant END = START.plus(1, ChronoUnit.HOURS);

    @Autowired
    MockMvc mvc;

    @Autowired
    VehicleRepository vehicles;

    private UUID vehicleId;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 25, Vehicle.Status.AVAILABLE));
    }

    @Test
    void createsBookingWith201() throws Exception {
        mvc.perform(book(vehicleId, guest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void ruleViolationIsAProblemDetailWithTheRuleId() throws Exception {
        mvc.perform(book(UUID.randomUUID(), guest()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.rule").value("BR1"))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:regra-de-negocio"))
                .andExpect(jsonPath("$.instance").value("/api/bookings"));
    }

    @Test
    void bookingConflictIs409WithBr7() throws Exception {
        mvc.perform(book(vehicleId, guest())).andExpect(status().isCreated());

        mvc.perform(book(vehicleId, guest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("BR7"));
    }

    @Test
    void malformedRequestIs400() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{\"guestId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private static String guest() {
        return "guest-" + UUID.randomUUID();
    }

    private static org.springframework.test.web.servlet.RequestBuilder book(UUID vehicle, String guest) {
        String body = """
                {"vehicleId":"%s","guestId":"%s","startsAt":"%s","endsAt":"%s"}
                """.formatted(vehicle, guest, START, END);
        return post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
