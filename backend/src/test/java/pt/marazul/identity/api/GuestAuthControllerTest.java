package pt.marazul.identity.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.marazul.identity.application.CodeHasher;
import pt.marazul.identity.application.GuestDirectory;
import pt.marazul.identity.application.GuestLoginService;
import pt.marazul.identity.application.LoginThrottle;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.Stay;
import pt.marazul.identity.infrastructure.InMemoryGuestDirectory;
import pt.marazul.identity.infrastructure.JwtTokenIssuer;
import pt.marazul.identity.infrastructure.Sha256CodeHasher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GuestAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "marazul.identity.jwt-secret=test-secret-with-at-least-32-bytes-ok")
@Import({GuestLoginService.class, LoginThrottle.class, InMemoryGuestDirectory.class, Sha256CodeHasher.class,
        JwtTokenIssuer.class, GuestAuthControllerTest.FixedClock.class})
class GuestAuthControllerTest {
    static final Instant NOW = Instant.parse("2026-12-01T12:50:00Z");
    private static final AtomicInteger ORIGINS = new AtomicInteger();

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
    GuestDirectory directory;
    @Autowired
    CodeHasher hasher;

    private final String origin = "203.0.113." + ORIGINS.incrementAndGet();

    private String guestCode(Instant checkIn, Instant checkOut) {
        String code = "CODE-" + UUID.randomUUID();
        Stay stay = new Stay(UUID.randomUUID(), "ref", checkIn, checkOut);
        directory.save(stay);
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Marta", hasher.hash(code)));
        return code;
    }

    private String openStayCode() {
        return guestCode(Instant.parse("2026-11-24T15:00:00Z"), Instant.parse("2026-12-01T11:00:00Z"));
    }

    private ResultActions login(String code) throws Exception {
        return mvc.perform(post("/api/auth/guest").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}")
                .with(r -> {
                    r.setRemoteAddr(origin);
                    return r;
                }));
    }

    @Test
    void aValidCodeReturnsATokenAndTheExpiry() throws Exception {
        login(openStayCode()).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Marta"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-01T13:00:00Z"));
    }

    @Test
    void aWrongCodeIsUnauthorized() throws Exception {
        login("NOPE-" + UUID.randomUUID()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Código inválido"));
    }

    @Test
    void aStayThatStartsTomorrowIsBr13() throws Exception {
        String code = guestCode(Instant.parse("2026-12-02T15:00:00Z"), Instant.parse("2026-12-09T11:00:00Z"));

        login(code).andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.rule").value("BR13"));
    }

    @Test
    void theSixthWrongAttemptIsRateLimited() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("NOPE-" + UUID.randomUUID()).andExpect(status().isUnauthorized());
        }

        login("NOPE-" + UUID.randomUUID()).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Demasiadas tentativas"));
    }

    @Test
    void anEmptyCodeIsABadRequest() throws Exception {
        login("").andExpect(status().isBadRequest());
    }

    @Test
    void theSessionEndpointAcceptsTheIssuedToken() throws Exception {
        String body = login(openStayCode()).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        String guestId = JsonPath.read(body, "$.guestId");

        mvc.perform(get("/api/auth/guest/session").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guestId").value(guestId))
                .andExpect(jsonPath("$.name").value("Marta"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-01T13:00:00Z"));
    }

    @Test
    void theSessionEndpointRejectsAMissingHeader() throws Exception {
        mvc.perform(get("/api/auth/guest/session")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Sessão inválida ou expirada"));
    }

    @Test
    void theSessionEndpointRejectsAnInvalidToken() throws Exception {
        mvc.perform(get("/api/auth/guest/session").header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }
}
