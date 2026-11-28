package pt.marazul.cucumber.steps;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.boot.test.web.server.LocalServerPort;
import pt.marazul.identity.api.IdentityController;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSteps {
    private final int port;
    private Response response;
    private Map<String, String> sessionCookies = Map.of();

    public ApiSteps(@LocalServerPort int port) {
        this.port = port;
    }

    @Before
    public void configureRestAssured() {
        RestAssured.port = port;
    }

    @Quando("consulto {string}")
    public void consulto(String path) {
        response = given().cookies(sessionCookies).accept("application/json, text/plain;q=0.9").get(path);
    }

    @Quando("envio para {string} o pedido:")
    public void envioOPedido(String path, String body) {
        response = post(path, body, sessionCookies);
    }

    @Quando("envio um PUT para {string} com o pedido:")
    public void envioUmPutPara(String path, String body) {
        response = withCsrf(body, sessionCookies).put(path);
    }

    public void autenticar(String email, String password) {
        Response login = post("/api/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", Map.of());
        assertThat(login.statusCode()).as(login.asPrettyString()).isEqualTo(200);
        sessionCookies = Map.of(IdentityController.AUTH_COOKIE, login.getCookie(IdentityController.AUTH_COOKIE));
    }

    private static Response post(String path, String body, Map<String, String> cookies) {
        return withCsrf(body, cookies).post(path);
    }

    private static RequestSpecification withCsrf(String body, Map<String, String> cookies) {
        Response csrf = given().get("/api/auth/csrf");
        return given().cookies(csrf.getCookies()).cookies(cookies)
                .header(csrf.jsonPath().getString("headerName"), csrf.jsonPath().getString("token"))
                .contentType(ContentType.JSON).accept(ContentType.JSON).body(body);
    }

    @Então("a resposta tem o estado {int}")
    public void aRespostaTemOEstado(int status) {
        assertThat(response.statusCode()).as(response.asPrettyString()).isEqualTo(status);
    }

    @Então("o campo {string} da resposta é {string}")
    public void oCampoDaRespostaE(String field, String expected) {
        assertThat(response.jsonPath().getString(field)).isEqualTo(expected);
    }

    public Response lastResponse() {
        return response;
    }
}
