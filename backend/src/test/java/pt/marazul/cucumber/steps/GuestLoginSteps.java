package pt.marazul.cucumber.steps;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import pt.marazul.cucumber.MutableClock;
import pt.marazul.identity.application.CodeHasher;
import pt.marazul.identity.application.GuestDirectory;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.Stay;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class GuestLoginSteps {
    private static final AtomicInteger ORIGINS = new AtomicInteger();
    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);

    private final MutableClock clock;
    private final GuestDirectory directory;
    private final CodeHasher hasher;
    private final String origin = "198.51.100." + ORIGINS.incrementAndGet();
    private final String code = "CODE-" + UUID.randomUUID();
    private String token;
    private Response last;

    public GuestLoginSteps(MutableClock clock, GuestDirectory directory, CodeHasher hasher) {
        this.clock = clock;
        this.directory = directory;
        this.hasher = hasher;
    }

    @Dado("uma estadia com check-out às {int}h{int} de 1 de dezembro e a hóspede Marta")
    public void umaEstadiaEAHospede(int hour, int minute) {
        Instant checkOut = at(hour, minute, 0);
        Stay stay = new Stay(UUID.randomUUID(), "ref-" + UUID.randomUUID(), checkOut.minusSeconds(7 * 86400L + 3600), checkOut);
        directory.save(stay);
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Marta", hasher.hash(code)));
    }

    @Dado("uma estadia que começa amanhã e a hóspede Marta")
    public void umaEstadiaQueComecaAmanha() {
        Instant checkIn = at(15, 0, 0).plusSeconds(86400);
        Stay stay = new Stay(UUID.randomUUID(), "ref-" + UUID.randomUUID(), checkIn, checkIn.plusSeconds(7 * 86400L));
        directory.save(stay);
        directory.save(new Guest(UUID.randomUUID(), stay.id(), "Marta", hasher.hash(code)));
    }

    @Dado("o relógio marca {int}h{int} de 1 de dezembro")
    public void oRelogioMarca(int hour, int minute) {
        clock.set(at(hour, minute, 0));
    }

    @Quando("o relógio marca {int}h{int} de 1 de dezembro e {int} s")
    public void oRelogioMarcaComSegundos(int hour, int minute, int second) {
        clock.set(at(hour, minute, second));
    }

    @Quando("a Marta entra com o seu código")
    public void aMartaEntra() {
        Response response = loginWith(code);
        token = response.statusCode() == 200 ? response.jsonPath().getString("token") : null;
    }

    @Quando("alguém tenta {int} códigos errados")
    public void alguemTentaCodigosErrados(int attempts) {
        for (int i = 0; i < attempts; i++) {
            loginWith("ERRADO-" + UUID.randomUUID());
        }
    }

    @Quando("a Marta consulta a sua sessão")
    public void aMartaConsultaASessao() {
        last = (given().accept(ContentType.JSON).header("X-Forwarded-For", origin)
                .header("Authorization", "Bearer " + token).get("/api/auth/guest/session"));
    }

    @Então("a resposta do hóspede tem o estado {int}")
    public void aRespostaTemOEstado(int status) {
        assertThat(last.statusCode()).as(last.asPrettyString()).isEqualTo(status);
    }

    @Então("o campo {string} da resposta do hóspede é {string}")
    public void oCampoDaRespostaE(String field, String expected) {
        assertThat(last.jsonPath().getString(field)).isEqualTo(expected);
    }

    private Response loginWith(String candidate) {
        Response response = given().contentType(ContentType.JSON).accept(ContentType.JSON)
                .header("X-Forwarded-For", origin)
                .body("{\"code\":\"" + candidate + "\"}").post("/api/auth/guest");
        last = response;
        return response;
    }

    private static Instant at(int hour, int minute, int second) {
        return DAY.atTime(LocalTime.of(hour, minute, second)).toInstant(ZoneOffset.UTC);
    }
}
