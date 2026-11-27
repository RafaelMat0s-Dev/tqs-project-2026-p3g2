package pt.marazul.cucumber.steps;

import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

public class TelemetrySteps {
    private static final Duration SLO3 = Duration.ofSeconds(5);
    private static MqttClient publisher;
    private static boolean adapterReady;

    @Value("${marazul.telemetry.broker-url}")
    private String brokerUrl;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    private final Map<String, Double> recorded = new HashMap<>();
    private ScheduledExecutorService background;
    private double discardedBefore;
    private String invalidMessage;

    // o contexto Spring é partilhado: cada cenário parte da B-17 sem estado
    @Before
    public void clearB17() {
        jdbc.update("delete from telemetry_vehicle_state where vehicle_code = 'B-17'");
    }

    @AfterAll
    public static synchronized void closePublisher() throws MqttException {
        if (publisher != null) {
            if (publisher.isConnected()) {
                publisher.disconnect();
            }
            publisher.close();
            publisher = null;
            adapterReady = false;
        }
    }

    @After
    public void stopBackgroundPublishing() {
        if (background != null) {
            background.shutdownNow();
        }
    }

    @Dado("que estão a ser publicadas {int} leituras por segundo de outras bicicletas")
    public void backgroundLoad(int perSecond) throws MqttException {
        ensureAdapterReady();
        var next = new AtomicInteger();
        background = Executors.newSingleThreadScheduledExecutor();
        background.scheduleAtFixedRate(() -> {
            var code = String.format("B-%02d", 1 + next.getAndIncrement() % 10);
            publishQuietly(code, json(code, clock.instant(), 80));
        }, 0, 1000 / perSecond, TimeUnit.MILLISECONDS);
    }

    @Dado("que há leituras a ser processadas")
    public void readingsAreBeingProcessed() throws MqttException {
        ensureAdapterReady();
    }

    @Dado("que a bicicleta {string} tem uma leitura de {int} % das {string}")
    public void vehicleHasReading(String code, int battery, String originAt) throws MqttException {
        ensureAdapterReady();
        publish(code, json(code, Instant.parse(originAt), battery));
        awaitBattery(code, battery);
    }

    @Quando("é publicada uma leitura de {int} % para a bicicleta {string}")
    public void readingPublished(int battery, String code) throws MqttException {
        ensureAdapterReady();
        publish(code, json(code, clock.instant(), battery));
    }

    @Quando("chega à bicicleta {string} uma leitura de {int} % das {string}")
    public void readingArrives(String code, int battery, String originAt) throws MqttException {
        discardedBefore = metric("marazul_telemetry_discarded_total");
        publish(code, json(code, Instant.parse(originAt), battery));
    }

    @Dado("uma mensagem com JSON inválido")
    public void invalidJsonMessage() {
        invalidMessage = "{isto não é json";
    }

    @Quando("chega ao adaptador de telemetria")
    public void messageArrives() throws MqttException {
        ensureAdapterReady();
        publish("B-17", invalidMessage);
    }

    @Então("a leitura é descartada")
    public void readingIsDiscarded() {
        await().atMost(SLO3).until(() -> metric("marazul_telemetry_discarded_total") > discardedBefore);
    }

    @Então("a mensagem é rejeitada")
    public void messageIsRejected() {
        metricIncreases("marazul_telemetry_rejected_total");
    }

    @Dado("que o valor atual da métrica {string} fica registado")
    public void recordMetric(String name) {
        recorded.put(name, metric(name));
    }

    @Dado("o valor atual da métrica {string} fica registado")
    public void recordMetricAgain(String name) {
        recordMetric(name);
    }

    @Então("a métrica {string} aumenta")
    public void metricIncreases(String name) {
        await().atMost(SLO3).until(() -> metric(name) > recorded.get(name));
    }

    @Então("em menos de 5 segundos a métrica {string} aumenta")
    public void metricIncreasesInTime(String name) {
        metricIncreases(name);
    }

    @Então("em menos de 5 segundos o estado da bicicleta {string} mostra {int} %")
    public void stateShows(String code, int battery) {
        awaitBattery(code, battery);
    }

    @Então("a métrica {string} está exposta")
    public void metricIsExposed(String name) {
        assertThat(prometheus()).contains("# TYPE " + name + " ");
    }

    private void awaitBattery(String code, int battery) {
        await().atMost(SLO3).untilAsserted(() -> given().get("/api/telemetry/vehicles/" + code)
                .then().statusCode(200).body("batteryPercentage", equalTo(battery)));
    }

    private static String prometheus() {
        return given().get("/actuator/prometheus").then().statusCode(200).extract().asString();
    }

    private static double metric(String name) {
        var matcher = Pattern.compile("(?m)^" + name + "(\\{[^}]*\\})?\\s+(\\S+)$").matcher(prometheus());
        return matcher.find() ? Double.parseDouble(matcher.group(2)) : 0;
    }

    // o adaptador subscreve de forma assíncrona: só a primeira leitura processada garante que está pronto
    private void ensureAdapterReady() throws MqttException {
        if (publisher == null) {
            publisher = new MqttClient(brokerUrl, "cucumber-publisher-" + UUID.randomUUID(), new MemoryPersistence());
            publisher.connect();
        }
        if (adapterReady) {
            return;
        }
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(300)).until(() -> {
            publishQuietly("B-WARMUP", json("B-WARMUP", clock.instant().truncatedTo(ChronoUnit.MILLIS), 90));
            return given().get("/api/telemetry/vehicles/B-WARMUP").statusCode() == 200;
        });
        adapterReady = true;
    }

    private static String json(String code, Instant originAt, int battery) {
        return "{\"vehicleId\":\"" + code + "\",\"originAt\":\"" + originAt + "\",\"batteryPercentage\":"
                + battery + ",\"locked\":true,\"hubId\":\"sao-bras\",\"shock\":false}";
    }

    private static synchronized void publish(String code, String payload) throws MqttException {
        var message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        publisher.publish("marazul/vehicles/" + code + "/telemetry", message);
    }

    private static void publishQuietly(String code, String payload) {
        try {
            publish(code, payload);
        } catch (MqttException e) {
            throw new IllegalStateException(e);
        }
    }
}
