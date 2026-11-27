package pt.marazul.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pt.marazul.identity.api.IdentityController;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffRole;
import pt.marazul.support.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleLifecycleIT {
    private static final String PASSWORD = "uma-palavra-passe-segura";

    @Autowired
    MockMvc mvc;

    @Autowired
    StaffAccountService accounts;

    private Cookie sessionOf(StaffRole role) throws Exception {
        String email = role.name().toLowerCase() + "-" + System.nanoTime() + "@marazul.test";
        accounts.create(email, "Rui Sampaio", PASSWORD, role);
        MvcResult login = mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(IdentityController.AUTH_COOKIE))
                .andReturn();
        return login.getResponse().getCookie(IdentityController.AUTH_COOKIE);
    }

    private static String body(String code, String hub) {
        return "{\"code\":\"" + code + "\",\"type\":\"E_BIKE\",\"baseHubCode\":\"" + hub + "\"}";
    }

    /** Regista um veículo com um código B-xxx livre (a base de dados é partilhada entre testes). */
    private MvcResult registerFresh(Cookie session, String hub) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = "B-" + ThreadLocalRandom.current().nextInt(100, 1000);
            MvcResult result = mvc.perform(post("/api/vehicles").with(csrf()).cookie(session)
                    .contentType(MediaType.APPLICATION_JSON).content(body(code, hub))).andReturn();
            if (result.getResponse().getStatus() == 201) {
                return result;
            }
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
        }
        throw new AssertionError("sem código B-xxx livre");
    }

    private static String field(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    @Test
    void vehicleGoesThroughTheWholeLifecycleAndRetiredCannotBeActivated() throws Exception {
        Cookie coordinator = sessionOf(StaffRole.COORDINATOR);
        MvcResult created = registerFresh(coordinator, "sao-bras");
        String id = field(created, "$.id");
        assertThat(created.getResponse().getHeader("Location")).isEqualTo("/api/vehicles/" + id);

        mvc.perform(get("/api/vehicles").cookie(coordinator).param("hub", "sao-bras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].status").value("DISABLED"));

        mvc.perform(patch("/api/vehicles/" + id + "/activate").with(csrf()).cookie(coordinator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(patch("/api/vehicles/" + id + "/deactivate").with(csrf()).cookie(coordinator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISABLED"));
        mvc.perform(patch("/api/vehicles/" + id + "/retire").with(csrf()).cookie(coordinator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRED"));

        mvc.perform(patch("/api/vehicles/" + id + "/activate").with(csrf()).cookie(coordinator))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.rule").value("VEHICLE_LIFECYCLE"))
                .andExpect(jsonPath("$.detail").value("Transição inválida: Abatida → Disponível"))
                .andExpect(jsonPath("$.instance").value("/api/vehicles/" + id + "/activate"));
    }

    @Test
    void duplicateCodeIs409WithRuleVehicleCode() throws Exception {
        Cookie technician = sessionOf(StaffRole.TECHNICIAN);
        String code = field(registerFresh(technician, "sao-bras"), "$.code");

        mvc.perform(post("/api/vehicles").with(csrf()).cookie(technician).contentType(MediaType.APPLICATION_JSON)
                        .content(body(code, "praia-da-ribeira")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rule").value("VEHICLE_CODE"));
    }

    @Test
    void listFiltersByStatusAndHub() throws Exception {
        Cookie coordinator = sessionOf(StaffRole.COORDINATOR);
        String hub = "hub-" + System.nanoTime();
        String active = field(registerFresh(coordinator, hub), "$.id");
        String inactive = field(registerFresh(coordinator, hub), "$.id");
        mvc.perform(patch("/api/vehicles/" + active + "/activate").with(csrf()).cookie(coordinator))
                .andExpect(status().isOk());

        mvc.perform(get("/api/vehicles").cookie(coordinator).param("hub", hub))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/vehicles").cookie(coordinator).param("hub", hub).param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active));
        mvc.perform(get("/api/vehicles").cookie(coordinator).param("hub", hub).param("status", "DISABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inactive));
    }

    @Test
    void unknownIdIs404AndMalformedIdIs400() throws Exception {
        Cookie coordinator = sessionOf(StaffRole.COORDINATOR);

        mvc.perform(patch("/api/vehicles/0b1c2d3e-0000-4000-8000-00000000dead/retire").with(csrf()).cookie(coordinator))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(patch("/api/vehicles/B-17/retire").with(csrf()).cookie(coordinator))
                .andExpect(status().isBadRequest());
    }

    @Test
    void clientManagerIsForbidden() throws Exception {
        Cookie manager = sessionOf(StaffRole.CLIENT_MANAGER);

        mvc.perform(get("/api/vehicles").cookie(manager)).andExpect(status().isForbidden());
        mvc.perform(post("/api/vehicles").with(csrf()).cookie(manager).contentType(MediaType.APPLICATION_JSON)
                        .content(body("B-17", "sao-bras")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"));
    }

    @Test
    void withoutSessionIs401() throws Exception {
        mvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void openApiListsTheVehiclePaths() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/vehicles']").exists())
                .andExpect(jsonPath("$.paths['/api/vehicles/{id}/activate']").exists());
    }
}
