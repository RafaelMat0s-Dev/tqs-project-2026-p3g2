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

        mvc.perform(put("/api/contract").with(csrf()).cookie(login.getResponse().getCookie(IdentityController.AUTH_COOKIE)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/contract"));

        // O técnico pode registar veículos (D6 da SCRUM-20); a gestora do cliente não.
        String managerEmail = "helena-" + System.nanoTime() + "@marazul.test";
        accounts.create(managerEmail, "Helena Gestora", PASSWORD, StaffRole.CLIENT_MANAGER);
        var managerCookie = login(managerEmail).getResponse().getCookie(IdentityController.AUTH_COOKIE);
        mvc.perform(post("/api/vehicles").with(csrf()).cookie(managerCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"B-17\",\"type\":\"E_BIKE\",\"baseHubCode\":\"hotel-alvina-mar\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/api/vehicles"));
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
        String coordinatorEmail = "fleet-coordinator-" + System.nanoTime() + "@marazul.test";
        accounts.create(coordinatorEmail, "Rui Sampaio", PASSWORD, StaffRole.COORDINATOR);
        var cookie = login(coordinatorEmail).getResponse().getCookie(IdentityController.AUTH_COOKIE);
        String adminEmail = "fleet-admin-" + System.nanoTime() + "@marazul.test";
        accounts.create(adminEmail, "Sofia Administradora", PASSWORD, StaffRole.ADMINISTRATOR);
        var adminCookie = login(adminEmail).getResponse().getCookie(IdentityController.AUTH_COOKIE);

        MvcResult created = null;
        for (int attempt = 0; attempt < 50 && (created == null || created.getResponse().getStatus() != 201); attempt++) {
            String code = "B-" + java.util.concurrent.ThreadLocalRandom.current().nextInt(100, 1000);
            created = mvc.perform(post("/api/vehicles").with(csrf()).cookie(cookie).contentType("application/json")
                    .content("{\"code\":\"" + code + "\",\"type\":\"E_BIKE\",\"baseHubCode\":\"hotel-alvina-mar\"}"))
                    .andReturn();
        }
        org.assertj.core.api.Assertions.assertThat(created.getResponse().getStatus()).isEqualTo(201);
        String id = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mvc.perform(put("/api/contract").with(csrf()).cookie(adminCookie)).andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/vehicles/" + id + "/retire")
                        .with(csrf()).cookie(cookie))
                .andExpect(status().isOk());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/vehicles/" + id + "/activate")
                        .with(csrf()).cookie(cookie))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.rule").value("VEHICLE_LIFECYCLE"))
                .andExpect(jsonPath("$.instance").value("/api/vehicles/" + id + "/activate"));
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
