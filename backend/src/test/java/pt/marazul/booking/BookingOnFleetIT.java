package pt.marazul.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.api.VehicleView;
import pt.marazul.support.FleetFixtures;
import pt.marazul.support.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BookingOnFleetIT {
    @Autowired MockMvc mvc;
    @Autowired FleetApi fleet;
    @Autowired ApplicationEventPublisher events;
    @Autowired Clock clock;

    private ResultActions book(UUID vehicleId) throws Exception {
        Instant start = clock.instant().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String body = "{\"vehicleId\":\"%s\",\"guestId\":\"marta-%s\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}"
                .formatted(vehicleId, UUID.randomUUID(), start, start.plus(1, ChronoUnit.HOURS));
        return mvc.perform(post("/api/bookings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private VehicleView.Status statusInFleet(UUID id) {
        return fleet.findById(id).orElseThrow().status();
    }

    @Test
    void aVehicleInMaintenanceIsRefusedWithBr1() throws Exception {
        UUID id = FleetFixtures.availableVehicle(fleet, events, 80);
        fleet.sendToMaintenance(id);

        book(id).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.rule").value("BR1"));
        assertThat(statusInFleet(id)).isEqualTo(VehicleView.Status.MAINTENANCE);
    }

    @Test
    void anAvailableVehicleWithoutReadingsIsRefusedWithBr1() throws Exception {
        UUID id = FleetFixtures.activeVehicle(fleet).id();

        book(id).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.rule").value("BR1"));
        assertThat(statusInFleet(id)).isEqualTo(VehicleView.Status.AVAILABLE);
    }

    @ParameterizedTest
    @CsvSource({"24,422", "25,201", "26,201"})
    void batteryBoundaryOf25PercentDecidesTheBooking(int battery, int expectedStatus) throws Exception {
        UUID id = FleetFixtures.availableVehicle(fleet, events, battery);

        ResultActions result = book(id).andExpect(status().is(expectedStatus));

        if (expectedStatus == 422) {
            result.andExpect(jsonPath("$.rule").value("BR1"));
            assertThat(statusInFleet(id)).isEqualTo(VehicleView.Status.AVAILABLE);
        }
    }

    @Test
    void aSuccessfulBookingLeavesTheVehicleReservedInTheFleet() throws Exception {
        UUID id = FleetFixtures.availableVehicle(fleet, events, 80);

        book(id).andExpect(status().isCreated()).andExpect(jsonPath("$.vehicleId").value(id.toString()));

        assertThat(statusInFleet(id)).isEqualTo(VehicleView.Status.RESERVED);
    }
}
