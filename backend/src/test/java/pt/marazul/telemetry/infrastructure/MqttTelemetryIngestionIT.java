package pt.marazul.telemetry.infrastructure;

import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.marazul.support.TestcontainersConfiguration;
import pt.marazul.telemetry.api.TelemetryReadingReceived;
import pt.marazul.telemetry.application.TelemetryIngestionService;
import pt.marazul.telemetry.application.TelemetrySource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class MqttTelemetryIngestionIT {
    private static final Duration SLO3 = Duration.ofSeconds(5);
    private static final String DELAY_COUNT = "marazul_telemetry_delay_seconds_count";

    @TestConfiguration(proxyBeanMethods = false)
    static class EventCaptureConfiguration {
        @Bean
        ReceivedEvents receivedEvents() {
            return new ReceivedEvents();
        }
    }

    static class ReceivedEvents {
        final List<TelemetryReadingReceived> events = new CopyOnWriteArrayList<>();

        @EventListener
        void on(TelemetryReadingReceived event) {
            events.add(event);
        }

        boolean contains(String vehicleCode) {
            return events.stream().anyMatch(event -> vehicleCode.equals(event.vehicleCode()));
        }
    }

    private static MqttClient publisher;
    private static boolean adapterReady;

    @LocalServerPort
    int port;

    @Value("${marazul.telemetry.broker-url}")
    String brokerUrl;

    @Autowired
    MeterRegistry registry;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    Clock clock;

    @Autowired
    ReceivedEvents received;

    @Autowired
    ApplicationContext context;

    @BeforeEach
    void setUp() throws MqttException {
        RestAssured.port = port;
        if (publisher == null) {
            publisher = connect();
        }
        jdbc.update("delete from telemetry_vehicle_state where vehicle_code <> 'B-WARMUP'");
        received.events.clear();
        waitForAdapterSubscription();
    }

    @AfterAll
    static void closePublisher() throws MqttException {
        if (publisher != null) {
            publisher.disconnect();
            publisher.close();
            publisher = null;
        }
    }

    @Test
    void readingPublishedUnderBackgroundLoadReachesStateEventAndMetrics() throws Exception {
        var delaysBefore = timerCount();
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var next = new AtomicInteger();
        try {
            scheduler.scheduleAtFixedRate(() -> {
                var other = "B-" + (30 + next.getAndIncrement() % 10);
                publishQuietly(other, json(other, clock.instant(), 80));
            }, 0, 100, TimeUnit.MILLISECONDS);

            publish("B-17", json("B-17", clock.instant(), 47));

            await().atMost(SLO3).untilAsserted(() -> given().get("/api/telemetry/vehicles/B-17")
                    .then().statusCode(200).body("batteryPercentage", org.hamcrest.Matchers.equalTo(47)));
            await().atMost(SLO3).until(() -> received.contains("B-17"));
            await().atMost(SLO3).until(() -> timerCount() > delaysBefore);
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    void olderReadingIsDiscardedAndStateDoesNotChange() throws Exception {
        publish("B-21", json("B-21", Instant.parse("2026-11-20T14:31:07Z"), 47));
        await().atMost(SLO3).untilAsserted(() -> given().get("/api/telemetry/vehicles/B-21")
                .then().statusCode(200).body("batteryPercentage", org.hamcrest.Matchers.equalTo(47)));
        var discardedBefore = counter("marazul.telemetry.discarded");

        publish("B-21", json("B-21", Instant.parse("2026-11-20T14:31:05Z"), 30));

        await().atMost(SLO3).until(() -> counter("marazul.telemetry.discarded") > discardedBefore);
        given().get("/api/telemetry/vehicles/B-21").then().statusCode(200)
                .body("batteryPercentage", org.hamcrest.Matchers.equalTo(47))
                .body("lastReadingAt", org.hamcrest.Matchers.equalTo("2026-11-20T14:31:07Z"));
    }

    @Test
    void invalidJsonIsRejectedAndTheNextValidReadingIsApplied() throws Exception {
        var rejectedBefore = counter("marazul.telemetry.rejected");

        publishRaw("B-22", "{isto não é json");
        await().atMost(SLO3).until(() -> counter("marazul.telemetry.rejected") > rejectedBefore);
        publish("B-22", json("B-22", clock.instant(), 47));

        await().atMost(SLO3).untilAsserted(() -> given().get("/api/telemetry/vehicles/B-22")
                .then().statusCode(200).body("batteryPercentage", org.hamcrest.Matchers.equalTo(47)));
    }

    @Test
    void messageWithVehicleIdDifferentFromTheTopicIsRejected() throws Exception {
        var rejectedBefore = counter("marazul.telemetry.rejected");

        publishRaw("B-23", json("B-24", clock.instant(), 47));

        await().atMost(SLO3).until(() -> counter("marazul.telemetry.rejected") > rejectedBefore);
        given().get("/api/telemetry/vehicles/B-24").then().statusCode(404);
        given().get("/api/telemetry/vehicles/B-23").then().statusCode(404);
    }

    @Test
    void prometheusExposesTheDelayHistogramAndTheRejectedCounter() {
        given().get("/actuator/prometheus").then().statusCode(200)
                .body(containsString("marazul_telemetry_delay_seconds_bucket"))
                .body(containsString("marazul_telemetry_rejected_total"))
                .body(containsString("marazul_telemetry_discarded_total"));
    }

    @Test
    void ingestionServiceIsASingleBeanWiredToASingleTelemetrySource() {
        assertThat(context.getBeansOfType(TelemetryIngestionService.class)).hasSize(1);
        assertThat(context.getBeansOfType(TelemetrySource.class)).hasSize(1);
    }

    // a primeira leitura só passa depois de o adaptador estar ligado e subscrito
    private void waitForAdapterSubscription() {
        if (adapterReady) {
            return;
        }
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(300)).until(() -> {
            publishQuietly("B-WARMUP", json("B-WARMUP", clock.instant().truncatedTo(ChronoUnit.MILLIS), 90));
            return given().get("/api/telemetry/vehicles/B-WARMUP").statusCode() == 200;
        });
        adapterReady = true;
    }

    private double timerCount() {
        var timer = registry.find("marazul.telemetry.delay").timer();
        return timer == null ? 0 : timer.count();
    }

    private double counter(String name) {
        var counter = registry.find(name).counter();
        return counter == null ? 0 : counter.count();
    }

    private MqttClient connect() throws MqttException {
        var client = new MqttClient(brokerUrl, "it-publisher-" + UUID.randomUUID(), new MemoryPersistence());
        client.connect();
        return client;
    }

    private static String json(String vehicleId, Instant originAt, int battery) {
        return "{\"vehicleId\":\"" + vehicleId + "\",\"originAt\":\"" + originAt + "\",\"batteryPercentage\":"
                + battery + ",\"locked\":true,\"hubId\":\"sao-bras\",\"shock\":false}";
    }

    private static void publish(String vehicleId, String json) throws MqttException {
        publishRaw(vehicleId, json);
    }

    private static synchronized void publishRaw(String vehicleId, String text) throws MqttException {
        var message = new MqttMessage(text.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        publisher.publish("marazul/vehicles/" + vehicleId + "/telemetry", message);
    }

    private static void publishQuietly(String vehicleId, String json) {
        try {
            publishRaw(vehicleId, json);
        } catch (MqttException e) {
            throw new IllegalStateException(e);
        }
    }
}
