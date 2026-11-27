package pt.marazul.identity.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffRole;
import pt.marazul.support.TestcontainersConfiguration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class CsrfBrowserFlowIT {
    private static final String PASSWORD = "uma-palavra-passe-segura";

    @LocalServerPort
    int port;

    @Autowired
    StaffAccountService accounts;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void csrfCookieIsHttpOnlyAndTheTokenComesInTheBody() {
        Response csrf = given().get("/api/auth/csrf");

        assertThat(csrf.statusCode()).isEqualTo(200);
        assertThat(csrf.getDetailedCookie("XSRF-TOKEN").isHttpOnly()).isTrue();
        assertThat(csrf.jsonPath().getString("headerName")).isEqualTo("X-XSRF-TOKEN");
        assertThat(csrf.jsonPath().getString("token")).isEqualTo(csrf.getCookie("XSRF-TOKEN"));
    }

    @Test
    void loginWithTheCsrfTokenStartsAStaffSession() {
        String email = "rui-" + System.nanoTime() + "@marazul.test";
        accounts.create(email, "Rui Sampaio", PASSWORD, StaffRole.COORDINATOR);
        Response csrf = given().get("/api/auth/csrf");

        given().cookies(csrf.getCookies())
                .header("X-XSRF-TOKEN", csrf.jsonPath().getString("token"))
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")
                .post("/api/auth/login")
                .then().statusCode(200)
                .cookie(IdentityController.AUTH_COOKIE)
                .body("role", equalTo("COORDINATOR"));
    }

    @Test
    void loginWithoutTheCsrfTokenIsRejected() {
        given().contentType(ContentType.JSON)
                .body("{\"email\":\"rui@marazul.test\",\"password\":\"" + PASSWORD + "\"}")
                .post("/api/auth/login")
                .then().statusCode(403)
                .body("type", equalTo("urn:marazul:erro:acesso-negado"));
    }
}
