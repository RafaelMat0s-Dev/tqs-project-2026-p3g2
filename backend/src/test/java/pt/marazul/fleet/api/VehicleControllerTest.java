package pt.marazul.fleet.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.marazul.fleet.application.VehicleNotFound;
import pt.marazul.identity.application.JwtService;
import pt.marazul.identity.infrastructure.SecurityConfiguration;
import pt.marazul.shared.api.ApiExceptionHandler;
import pt.marazul.shared.domain.BusinessRuleConflict;
import pt.marazul.shared.domain.BusinessRuleViolation;

@WebMvcTest(VehicleController.class)
@Import({SecurityConfiguration.class, ApiExceptionHandler.class})
@WithMockUser(roles = "COORDINATOR")
class VehicleControllerTest {
    private static final UUID ID = UUID.fromString("0b1c2d3e-0000-4000-8000-000000000017");
    private static final String BODY = "{\"code\":\"B-17\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    FleetApi fleet;

    @MockitoBean
    JwtService jwt;

    private static VehicleView view(VehicleView.Status status) {
        return new VehicleView(ID, "B-17", "E_BIKE", "sao-bras", status, null, null);
    }

    private ResultActions register(String body) throws Exception {
        return mvc.perform(post("/api/vehicles").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions transition(String action, String id) throws Exception {
        return mvc.perform(patch("/api/vehicles/" + id + "/" + action).with(csrf()));
    }

    // --- POST /api/vehicles

    @Test
    void registerReturns201WithLocationAndDisabledView() throws Exception {
        when(fleet.register("B-17", "E_BIKE", "sao-bras")).thenReturn(view(VehicleView.Status.DISABLED));

        register(BODY).andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/vehicles/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.code").value("B-17"))
                .andExpect(jsonPath("$.type").value("E_BIKE"))
                .andExpect(jsonPath("$.baseHubCode").value("sao-bras"))
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.batteryPercentage").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.lastReadingAt").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void duplicateCodeIs409WithRuleVehicleCode() throws Exception {
        when(fleet.register(any(), any(), any()))
                .thenThrow(new BusinessRuleConflict("VEHICLE_CODE", "Já existe um veículo com o código B-17"));

        register(BODY).andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.rule").value("VEHICLE_CODE"))
                .andExpect(jsonPath("$.instance").value("/api/vehicles"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"code\":\"b-17\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"B-1\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"B-1234\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"X-17\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"\",\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"type\":\"E_BIKE\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"B-17\",\"type\":\"\",\"baseHubCode\":\"sao-bras\"}",
            "{\"code\":\"B-17\",\"type\":\"E_BIKE\",\"baseHubCode\":\"\"}",
            "{\"code\":\"B-17\",\"type\":\"E_BIKE\"}",
            "not json"})
    void invalidRegisterBodyIs400ProblemDetailAndNothingIsRegistered(String body) throws Exception {
        register(body).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verify(fleet, never()).register(any(), any(), any());
    }

    @ParameterizedTest
    @CsvSource({"32,1,201", "33,1,400", "1,64,201", "1,65,400"})
    void typeAndBaseHubCodeLengthBoundaries(int typeLength, int hubLength, int expected) throws Exception {
        String body = "{\"code\":\"B-17\",\"type\":\"" + "T".repeat(typeLength)
                + "\",\"baseHubCode\":\"" + "h".repeat(hubLength) + "\"}";
        when(fleet.register(any(), any(), any())).thenReturn(view(VehicleView.Status.DISABLED));

        register(body).andExpect(status().is(expected));
    }

    // --- PATCH transitions

    @ParameterizedTest
    @CsvSource({"activate,AVAILABLE", "deactivate,DISABLED", "retire,RETIRED"})
    void transitionReturns200WithTheView(String action, VehicleView.Status resulting) throws Exception {
        VehicleView result = view(resulting);
        when(fleet.activate(ID)).thenReturn(result);
        when(fleet.deactivate(ID)).thenReturn(result);
        when(fleet.retire(ID)).thenReturn(result);

        transition(action, ID.toString()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.status").value(resulting.name()));
    }

    @Test
    void invalidTransitionIs422WithRuleAndTransitionInDetail() throws Exception {
        when(fleet.activate(ID)).thenThrow(
                new BusinessRuleViolation("VEHICLE_LIFECYCLE", "Transição inválida: Abatida → Disponível"));

        transition("activate", ID.toString()).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.rule").value("VEHICLE_LIFECYCLE"))
                .andExpect(jsonPath("$.detail").value("Transição inválida: Abatida → Disponível"))
                .andExpect(jsonPath("$.instance").value("/api/vehicles/" + ID + "/activate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"activate", "deactivate", "retire"})
    void unknownVehicleIs404ProblemDetail(String action) throws Exception {
        when(fleet.activate(ID)).thenThrow(new VehicleNotFound(ID));
        when(fleet.deactivate(ID)).thenThrow(new VehicleNotFound(ID));
        when(fleet.retire(ID)).thenThrow(new VehicleNotFound(ID));

        transition(action, ID.toString()).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"activate", "deactivate", "retire"})
    void idThatIsNotAUuidIs400(String action) throws Exception {
        transition(action, "B-17").andExpect(status().isBadRequest());

        verifyNoInteractions(fleet);
    }

    // --- GET /api/vehicles

    @Test
    void listWithoutFiltersReturnsAllViews() throws Exception {
        when(fleet.list(Optional.empty(), Optional.empty()))
                .thenReturn(List.of(view(VehicleView.Status.DISABLED),
                        new VehicleView(UUID.randomUUID(), "B-18", "BICYCLE", "sao-bras",
                                VehicleView.Status.AVAILABLE, 80, Instant.parse("2026-11-20T14:31:07Z"))));

        mvc.perform(get("/api/vehicles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("B-17"))
                .andExpect(jsonPath("$[1].batteryPercentage").value(80))
                .andExpect(jsonPath("$[1].lastReadingAt").value("2026-11-20T14:31:07Z"));
    }

    @Test
    void listPassesStatusAndHubFilters() throws Exception {
        when(fleet.list(Optional.of(VehicleView.Status.AVAILABLE), Optional.of("sao-bras")))
                .thenReturn(List.of(view(VehicleView.Status.AVAILABLE)));

        mvc.perform(get("/api/vehicles").param("status", "AVAILABLE").param("hub", "sao-bras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void invalidStatusFilterIs400NotAServerError() throws Exception {
        mvc.perform(get("/api/vehicles").param("status", "FLYING")).andExpect(status().isBadRequest());

        verifyNoInteractions(fleet);
    }

    // --- segurança

    private static MockHttpServletRequestBuilder[] allOperations() {
        return new MockHttpServletRequestBuilder[] {
                post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(BODY),
                get("/api/vehicles"),
                patch("/api/vehicles/" + ID + "/activate"),
                patch("/api/vehicles/" + ID + "/deactivate"),
                patch("/api/vehicles/" + ID + "/retire")};
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianIsAllowedOnEveryOperation() throws Exception {
        when(fleet.register(any(), any(), any())).thenReturn(view(VehicleView.Status.DISABLED));
        when(fleet.activate(ID)).thenReturn(view(VehicleView.Status.AVAILABLE));
        when(fleet.deactivate(ID)).thenReturn(view(VehicleView.Status.DISABLED));
        when(fleet.retire(ID)).thenReturn(view(VehicleView.Status.RETIRED));

        for (var request : allOperations()) {
            mvc.perform(request.with(csrf())).andExpect(status().is2xxSuccessful());
        }
    }

    @Test
    void coordinatorIsAllowedOnEveryOperation() throws Exception {
        when(fleet.register(any(), any(), any())).thenReturn(view(VehicleView.Status.DISABLED));
        when(fleet.activate(ID)).thenReturn(view(VehicleView.Status.AVAILABLE));
        when(fleet.deactivate(ID)).thenReturn(view(VehicleView.Status.DISABLED));
        when(fleet.retire(ID)).thenReturn(view(VehicleView.Status.RETIRED));

        for (var request : allOperations()) {
            mvc.perform(request.with(csrf())).andExpect(status().is2xxSuccessful());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMINISTRATOR", "CLIENT_MANAGER"})
    void otherStaffRolesAreForbiddenOnEveryOperation(String role) throws Exception {
        for (var request : allOperations()) {
            mvc.perform(request.with(csrf()).with(org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.user("x").roles(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verifyNoInteractions(fleet);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorIsForbiddenEvenWhenTheRequestIsInvalid() throws Exception {
        register("not json").andExpect(status().isForbidden());
    }

    @Test
    void withoutSessionEveryOperationIs401() throws Exception {
        for (var request : allOperations()) {
            mvc.perform(request.with(csrf()).with(org.springframework.security.test.web.servlet.request
                            .SecurityMockMvcRequestPostProcessors.anonymous()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }

        verifyNoInteractions(fleet);
    }

    @Test
    void postWithoutCsrfTokenIs403AndRegistersNothing() throws Exception {
        mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());

        verify(fleet, never()).register(any(), any(), any());
    }

    @Test
    void patchWithoutCsrfTokenIs403AndChangesNothing() throws Exception {
        mvc.perform(patch("/api/vehicles/" + ID + "/retire")).andExpect(status().isForbidden());

        verify(fleet, never()).retire(any());
    }
}
