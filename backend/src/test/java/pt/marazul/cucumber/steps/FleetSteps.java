package pt.marazul.cucumber.steps;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.context.ApplicationEventPublisher;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.api.VehicleView;
import pt.marazul.telemetry.api.TelemetryReadingReceived;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class FleetSteps {
    private static final Instant READING_AT = Instant.parse("2026-11-20T14:31:07Z");

    /** The shared database needs unique codes, so the feature's alias maps to a real one per scenario. */
    private final Map<String, VehicleView> bikes = new HashMap<>();
    private final ApiSteps api;
    private final AuthSteps auth;
    private final FleetApi fleet;
    private final ApplicationEventPublisher events;

    public FleetSteps(ApiSteps api, AuthSteps auth, FleetApi fleet, ApplicationEventPublisher events) {
        this.api = api;
        this.auth = auth;
        this.fleet = fleet;
        this.events = events;
    }

    @Dado("a bicicleta {string} acabada de registar no hub {string}")
    public void freshBike(String alias, String hub) {
        register(alias, hub);
    }

    @Dado("a bicicleta {string} está {string}")
    public void bikeIs(String alias, String state) {
        VehicleView bike = register(alias, "hotel-alvina-mar");
        switch (state) {
            case "Abatida" -> fleet.retire(bike.id());
            case "Reservada" -> {
                fleet.activate(bike.id());
                fleet.markReserved(bike.id());
            }
            default -> throw new IllegalArgumentException("Estado não suportado: " + state);
        }
    }

    @Dado("a bicicleta {string} está {string} no hub {string}")
    public void bikeIsAtHub(String alias, String state, String hub) {
        if (!"Manutenção".equals(state)) {
            throw new IllegalArgumentException("Estado não suportado: " + state);
        }
        VehicleView bike = register(alias, hub);
        fleet.activate(bike.id());
        events.publishEvent(new TelemetryReadingReceived(bike.code(), READING_AT, 80, true, hub, false));
        fleet.sendToMaintenance(bike.id());
    }

    @Quando("o Rui consulta a bicicleta {string}")
    public void consult(String alias) {
        VehicleView bike = bikes.get(alias);
        Response list = authenticated().accept(ContentType.JSON).queryParam("hub", bike.baseHubCode()).get("/api/vehicles");
        if (list.statusCode() != 200) {
            api.remember(list);
            return;
        }
        // The shared assertions read a single object, so keep just this bike's JSON.
        Map<String, Object> mine = list.jsonPath().getMap("find { it.id == '" + bike.id() + "' }");
        assertThat(mine).as("bicicleta %s na lista: %s", alias, list.asString()).isNotNull();
        api.remember(new io.restassured.builder.ResponseBuilder().clone(list).setBody(toJson(mine)).build());
    }

    @Quando("o Rui ativa a bicicleta {string}")
    public void activate(String alias) {
        patch(alias, "activate");
    }

    @Quando("o Rui desativa a bicicleta {string}")
    public void deactivate(String alias) {
        patch(alias, "deactivate");
    }

    @Quando("a Marta tenta reservar a bicicleta {string}")
    public void reserve(String alias) {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        String body = "{\"vehicleId\":\"%s\",\"guestId\":\"Marta\",\"startsAt\":\"%s\",\"endsAt\":\"%s\"}"
                .formatted(bikes.get(alias).id(), start, start.plus(2, ChronoUnit.HOURS));
        api.remember(authenticated().header(auth.csrfHeaderName(), auth.csrfToken())
                .contentType(ContentType.JSON).accept(ContentType.JSON).body(body).post("/api/bookings"));
    }

    @Então("a mensagem da resposta contém {string}")
    public void messageContains(String text) {
        assertThat(api.lastResponse().jsonPath().getString("detail")).contains(text);
    }

    private VehicleView register(String alias, String hub) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = "B-" + ThreadLocalRandom.current().nextInt(100, 1000);
            if (fleet.findByCode(code).isEmpty()) {
                VehicleView bike = fleet.register(code, "E_BIKE", hub);
                bikes.put(alias, bike);
                return bike;
            }
        }
        throw new IllegalStateException("sem código B-xx livre");
    }

    private void patch(String alias, String action) {
        api.remember(authenticated().header(auth.csrfHeaderName(), auth.csrfToken()).accept(ContentType.JSON)
                .patch("/api/vehicles/{id}/" + action, bikes.get(alias).id()));
    }

    /** Session and CSRF cookies are sent explicitly: the session cookie is Secure (D16). */
    private RequestSpecification authenticated() {
        RequestSpecification spec = given().cookie("XSRF-TOKEN", auth.csrfCookie());
        return auth.sessionCookie() == null ? spec : spec.cookie("marazul_session", auth.sessionCookie());
    }

    private static String toJson(Map<String, Object> map) {
        return new tools.jackson.databind.json.JsonMapper().writeValueAsString(map);
    }
}
