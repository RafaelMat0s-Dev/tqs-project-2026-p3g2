package pt.marazul.cucumber.steps;

import io.cucumber.java.pt.Dado;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import pt.marazul.identity.api.IdentityController;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffRole;

import java.util.concurrent.atomic.AtomicLong;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class AuthSteps {
    private static final String PASSWORD = "uma-palavra-passe-segura";
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final StaffAccountService accounts;
    private String sessionCookie;
    private String csrfCookie;
    private String csrfHeader;
    private String csrfToken;

    public AuthSteps(StaffAccountService accounts) {
        this.accounts = accounts;
    }

    @Dado("estou autenticado como coordenador")
    public void coordinator() {
        login(StaffRole.COORDINATOR);
    }

    @Dado("estou autenticado como técnico")
    public void technician() {
        login(StaffRole.TECHNICIAN);
    }

    @Dado("estou autenticado como gestora do cliente")
    public void clientManager() {
        login(StaffRole.CLIENT_MANAGER);
    }

    /** Guests have no login yet: /api/bookings is open (D12). */
    @Dado("estou autenticado como hóspede")
    public void guest() {
        sessionCookie = null;
        fetchCsrf();
    }

    /** Session cookie (may be null for a guest), sent explicitly because it is Secure (D16). */
    public String sessionCookie() {
        return sessionCookie;
    }

    public String csrfCookie() {
        return csrfCookie;
    }

    public String csrfHeaderName() {
        return csrfHeader;
    }

    public String csrfToken() {
        return csrfToken;
    }

    private void login(StaffRole role) {
        String email = role.name().toLowerCase() + "-" + System.nanoTime() + "-" + SEQUENCE.incrementAndGet() + "@marazul.test";
        accounts.create(email, "Rui Sampaio", PASSWORD, role);
        fetchCsrf();
        Response response = given().cookie("XSRF-TOKEN", csrfCookie).header(csrfHeader, csrfToken)
                .contentType(ContentType.JSON).body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                .post("/api/auth/login");
        assertThat(response.statusCode()).as(response.asPrettyString()).isEqualTo(200);
        sessionCookie = response.getDetailedCookie(IdentityController.AUTH_COOKIE).getValue();
        // The login may rotate the CSRF token.
        if (response.getCookies().containsKey("XSRF-TOKEN")) {
            fetchCsrf();
        }
    }

    private void fetchCsrf() {
        Response csrf = given().get("/api/auth/csrf");
        csrfCookie = csrf.getCookie("XSRF-TOKEN");
        csrfHeader = csrf.jsonPath().getString("headerName");
        csrfToken = csrf.jsonPath().getString("token");
    }
}
