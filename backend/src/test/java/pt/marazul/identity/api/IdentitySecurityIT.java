package pt.marazul.identity.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.application.JwtService;
import pt.marazul.identity.domain.StaffRole;
import pt.marazul.support.TestcontainersConfiguration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IdentitySecurityIT {
    private static final String PASSWORD = "uma-palavra-passe-segura";
    private static final String CONTRACT_BODY = "{\"unlockFeeCents\":100,\"perMinuteCents\":5,\"depositCents\":2500,"
            + "\"surchargePerMinuteCents\":10,\"surchargeToleranceMinutes\":5,\"surchargeCapCents\":2500,"
            + "\"slaCriticalHours\":4,\"slaHighHours\":24,\"slaNormalHours\":72,"
            + "\"minimumBatteryPercentage\":25,\"pickupWindowMinutes\":15}";

    @Autowired
    MockMvc mvc;

    @Autowired
    StaffAccountService accounts;

    @Autowired
    JwtService jwt;

    @Test
    void technicianCannotUpdateContract() throws Exception {
        String email = "tech-" + System.nanoTime() + "@marazul.test";
        accounts.create(email, "Nídia Tavares", PASSWORD, StaffRole.TECHNICIAN);

        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(IdentityController.AUTH_COOKIE))
                .andReturn();

        var token = login.getResponse().getCookie(IdentityController.AUTH_COOKIE).getValue();
        org.assertj.core.api.Assertions.assertThat(jwt.parse(token)).isPresent();

        mvc.perform(put("/api/contract").with(csrf()).cookie(login.getResponse().getCookie(IdentityController.AUTH_COOKIE))
                        .contentType(MediaType.APPLICATION_JSON).content(CONTRACT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/contract"));

        mvc.perform(post("/api/vehicles").with(csrf()).cookie(login.getResponse().getCookie(IdentityController.AUTH_COOKIE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hubId\":\"hotel-alvina-mar\",\"type\":\"E_BIKE\",\"batteryPercentage\":80}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/api/vehicles"));
    }

    @Test
    void clientManagerCannotUpdateContract() throws Exception {
        String email = "helena-" + System.nanoTime() + "@marazul.test";
        accounts.create(email, "Helena Gestora", PASSWORD, StaffRole.CLIENT_MANAGER);
        var cookie = login(email).getResponse().getCookie(IdentityController.AUTH_COOKIE);

        mvc.perform(put("/api/contract").with(csrf()).cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON).content(CONTRACT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"));
    }

    @Test
    void administratorCreatesStaffAccountAndDuplicateEmailIsRejected() throws Exception {
        String adminEmail = "admin-" + System.nanoTime() + "@marazul.test";
        accounts.create(adminEmail, "Sofia Administradora", PASSWORD, StaffRole.ADMINISTRATOR);
        MvcResult login = login(adminEmail);
        var cookie = login.getResponse().getCookie(IdentityController.AUTH_COOKIE);
        String email = "nidia-" + System.nanoTime() + "@marazul.test";
        String body = "{\"email\":\"" + email
                + "\",\"displayName\":\"Nídia Tavares\",\"password\":\"" + PASSWORD
                + "\",\"role\":\"TECHNICIAN\"}";

        mvc.perform(post("/api/auth/staff").with(csrf()).cookie(cookie).contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/staff").with(csrf()).cookie(cookie).contentType("application/json").content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void e401VehicleFlowReturnsProblemDetailsForIllegalActivation() throws Exception {
        String adminEmail = "fleet-admin-" + System.nanoTime() + "@marazul.test";
        accounts.create(adminEmail, "Sofia Administradora", PASSWORD, StaffRole.ADMINISTRATOR);
        var cookie = login(adminEmail).getResponse().getCookie(IdentityController.AUTH_COOKIE);

        MvcResult created = mvc.perform(post("/api/vehicles").with(csrf()).cookie(cookie).contentType("application/json")
                        .content("{\"hubId\":\"hotel-alvina-mar\",\"type\":\"E_BIKE\",\"batteryPercentage\":80}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/vehicles/" + id + "/status")
                        .with(csrf()).cookie(cookie).contentType("application/json")
                        .content("{\"status\":\"RETIRED\"}"))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/vehicles/" + id + "/status")
                        .with(csrf()).cookie(cookie).contentType("application/json")
                        .content("{\"status\":\"AVAILABLE\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.rule").value("BR-E4-01"))
                .andExpect(jsonPath("$.instance").value("/api/vehicles/" + id + "/status"));
    }

    @Test
    void staffChangesWithoutCsrfTokenAreRejected() throws Exception {
        String adminEmail = "csrf-admin-" + System.nanoTime() + "@marazul.test";
        accounts.create(adminEmail, "Sofia Administradora", PASSWORD, StaffRole.ADMINISTRATOR);
        var cookie = login(adminEmail).getResponse().getCookie(IdentityController.AUTH_COOKIE);
        String body = "{\"email\":\"rui-" + System.nanoTime()
                + "@marazul.test\",\"displayName\":\"Rui Sampaio\",\"password\":\"" + PASSWORD
                + "\",\"role\":\"COORDINATOR\"}";

        mvc.perform(post("/api/auth/staff").cookie(cookie).contentType("application/json").content(body))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"));
    }

    private MvcResult login(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(IdentityController.AUTH_COOKIE))
                .andReturn();
    }
}
