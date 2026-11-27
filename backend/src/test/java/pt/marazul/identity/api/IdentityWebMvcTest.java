package pt.marazul.identity.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.marazul.contract.api.ContractController;
import pt.marazul.identity.application.JwtService;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.infrastructure.SecurityConfiguration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {IdentityController.class, ContractController.class})
@Import(SecurityConfiguration.class)
class IdentityWebMvcTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    StaffAccountService accounts;

    @MockitoBean
    JwtService jwt;

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianCannotCreateStaffAccount() throws Exception {
        mvc.perform(post("/api/auth/staff").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nidia@marazul.test\",\"displayName\":\"Nídia\","
                                + "\"password\":\"uma-palavra-passe-segura\",\"role\":\"TECHNICIAN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"))
                .andExpect(jsonPath("$.title").value("Acesso negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/api/auth/staff"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianCannotUpdateContract() throws Exception {
        mvc.perform(put("/api/contract").with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:acesso-negado"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value("/api/contract"));
    }

    @Test
    void unauthenticatedUserReceivesProblemDetails() throws Exception {
        mvc.perform(put("/api/contract").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:marazul:erro:nao-autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.instance").value("/api/contract"));
    }
}
