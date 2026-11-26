package pt.marazul.telemetry.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.marazul.telemetry.application.TelemetryEvents;
import pt.marazul.telemetry.application.TelemetryIngestionService;
import pt.marazul.telemetry.application.TelemetryMetrics;
import pt.marazul.telemetry.application.VehicleTelemetryRepository;
import pt.marazul.telemetry.domain.TelemetryReading;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TelemetryController.class)
@Import(TelemetryControllerTest.ServiceConfiguration.class)
class TelemetryControllerTest {
    @TestConfiguration
    static class ServiceConfiguration {
        @Bean
        TelemetryIngestionService telemetryIngestionService(VehicleTelemetryRepository repository,
                                                            TelemetryMetrics metrics, TelemetryEvents events) {
            return new TelemetryIngestionService(repository, metrics, events,
                    Clock.fixed(Instant.parse("2026-11-27T14:31:08Z"), ZoneOffset.UTC));
        }
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    VehicleTelemetryRepository repository;

    @MockitoBean
    TelemetryMetrics metrics;

    @MockitoBean
    TelemetryEvents events;

    @Test
    void knownVehicleReturns200WithTheFullState() throws Exception {
        when(repository.findLatest("B-17")).thenReturn(Optional.of(
                new TelemetryReading("B-17", Instant.parse("2026-11-27T14:31:07Z"), 47, true, "sao-bras", false)));

        mvc.perform(get("/api/telemetry/vehicles/{code}", "B-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleCode").value("B-17"))
                .andExpect(jsonPath("$.batteryPercentage").value(47))
                .andExpect(jsonPath("$.locked").value(true))
                .andExpect(jsonPath("$.hubId").value("sao-bras"))
                .andExpect(jsonPath("$.shock").value(false))
                .andExpect(jsonPath("$.lastReadingAt").value("2026-11-27T14:31:07Z"));
    }

    @Test
    void unknownVehicleReturns404ProblemDetails() throws Exception {
        mvc.perform(get("/api/telemetry/vehicles/{code}", "B-99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void oddlyFormattedCodeReturns404NotServerError() throws Exception {
        mvc.perform(get("/api/telemetry/vehicles/{code}", "x y"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
