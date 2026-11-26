package pt.marazul.cucumber.steps;

import io.cucumber.java.Before;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSteps {
    private final int port;
    private Response response;

    public ApiSteps(@LocalServerPort int port) {
        this.port = port;
    }

    @Before
    public void configureRestAssured() {
        RestAssured.port = port;
    }

    @Quando("consulto {string}")
    public void consulto(String path) {
        response = given().accept(ContentType.JSON).get(path);
    }

    @Quando("envio para {string} o pedido:")
    public void envioOPedido(String path, String body) {
        response = given().contentType(ContentType.JSON).accept(ContentType.JSON).body(body).post(path);
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
