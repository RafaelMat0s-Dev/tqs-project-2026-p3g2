package pt.marazul.contract.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.marazul.contract.application.ContractService;
import pt.marazul.contract.application.ContractVersionRepository;
import pt.marazul.identity.application.JwtService;
import pt.marazul.identity.infrastructure.SecurityConfiguration;
import pt.marazul.support.InMemoryContractVersionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContractController.class)
@Import({SecurityConfiguration.class, JwtService.class, ContractService.class, ContractControllerTest.Fixtures.class})
class ContractControllerTest {
    static final Instant NOW = Instant.parse("2026-12-01T09:00:00Z");

    @TestConfiguration(proxyBeanMethods = false)
    static class Fixtures {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }

        @Bean
        ContractVersionRepository contractVersionRepository() {
            return new InMemoryContractVersionRepository();
        }
    }

    @Autowired
    MockMvc mvc;

    private static String body(String field, int value) {
        String json = "{\"unlockFeeCents\":100,\"perMinuteCents\":7,\"depositCents\":2500,"
                + "\"surchargePerMinuteCents\":10,\"surchargeToleranceMinutes\":5,\"surchargeCapCents\":2500,"
                + "\"slaCriticalHours\":4,\"slaHighHours\":24,\"slaNormalHours\":72,"
                + "\"minimumBatteryPercentage\":25,\"pickupWindowMinutes\":15}";
        return field == null ? json : json.replaceAll("\"" + field + "\":-?\\d+", "\"" + field + "\":" + value);
    }

    private MockHttpServletRequestBuilder publish(String json) {
        return post("/api/contract/versions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void operatorSeesTheContractV1() throws Exception {
        mvc.perform(get("/api/contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.unlockFeeCents").value(100))
                .andExpect(jsonPath("$.perMinuteCents").value(5))
                .andExpect(jsonPath("$.depositCents").value(2500))
                .andExpect(jsonPath("$.surchargePerMinuteCents").value(10))
                .andExpect(jsonPath("$.surchargeToleranceMinutes").value(5))
                .andExpect(jsonPath("$.surchargeCapCents").value(2500))
                .andExpect(jsonPath("$.slaCriticalHours").value(4))
                .andExpect(jsonPath("$.slaHighHours").value(24))
                .andExpect(jsonPath("$.slaNormalHours").value(72))
                .andExpect(jsonPath("$.minimumBatteryPercentage").value(25))
                .andExpect(jsonPath("$.pickupWindowMinutes").value(15));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRATOR", "COORDINATOR", "CLIENT_MANAGER"})
    void staffWhoManageOperationsAndClientsCanReadTheContract(String role) throws Exception {
        mvc.perform(get("/api/contract").with(user("staff").roles(role)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianCannotReadTheContract() throws Exception {
        mvc.perform(get("/api/contract")).andExpect(status().isForbidden());
    }

    @Test
    void readingWithoutASessionIs401() throws Exception {
        mvc.perform(get("/api/contract"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void publishingWithoutASessionIs401() throws Exception {
        mvc.perform(publish(body(null, 0))).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENT_MANAGER", "TECHNICIAN", "COORDINATOR"})
    void onlyTheAdministratorPublishes(String role) throws Exception {
        mvc.perform(publish(body(null, 0)).with(user("staff").roles(role)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorPublishesVersion2AndVersion1IsUntouched() throws Exception {
        mvc.perform(publish(body(null, 0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.perMinuteCents").value(7))
                .andExpect(jsonPath("$.effectiveFrom").value("2026-12-01T09:00:00Z"));

        mvc.perform(get("/api/contract")).andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/contract/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.perMinuteCents").value(5));
    }

    @Test
    @WithMockUser(roles = "CLIENT_MANAGER")
    void aVersionCanBeReadByNumber() throws Exception {
        mvc.perform(get("/api/contract/versions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlockFeeCents").value(100));
    }

    @Test
    @WithMockUser(roles = "CLIENT_MANAGER")
    void anUnknownVersionIs404ProblemDetails() throws Exception {
        mvc.perform(get("/api/contract/versions/9"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @ParameterizedTest(name = "{0}={1} dá 422 com a regra {2}")
    @CsvSource({
            "unlockFeeCents, -1, BR10",
            "perMinuteCents, -1, BR10",
            "depositCents, -1, BR9",
            "surchargePerMinuteCents, -1, BR11",
            "surchargeToleranceMinutes, -1, BR11",
            "surchargeCapCents, -1, BR11",
            "slaCriticalHours, 0, BR15",
            "slaHighHours, 0, BR15",
            "slaNormalHours, 0, BR15",
            "minimumBatteryPercentage, 101, BR1",
            "pickupWindowMinutes, 0, BR6"})
    @WithMockUser(roles = "ADMINISTRATOR")
    void invalidValuesAreProblemDetailsWithTheRule(String field, int value, String rule) throws Exception {
        mvc.perform(publish(body(field, value)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.rule").value(rule))
                .andExpect(jsonPath("$.instance").value("/api/contract/versions"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void aDateNotAfterTheLatestVersionIs422Br10() throws Exception {
        mvc.perform(publish(body(null, 0).replace("{", "{\"effectiveFrom\":\"1970-01-01T00:00:00Z\",")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.rule").value("BR10"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void aMissingFieldIs400() throws Exception {
        mvc.perform(publish(body(null, 0).replace("\"perMinuteCents\":7,", "")))
                .andExpect(status().isBadRequest());
    }
}
