package pt.marazul.telemetry.infrastructure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MqttTelemetryAdapterLifecycleTest {
    @Test
    void stopLeavesNoClientThreadsWhileTheConnectionIsStillPending() throws IOException {
        var prefix = "lifecycle-" + UUID.randomUUID() + "-";
        // aceita a ligação TCP mas nunca responde com CONNACK: a ligação MQTT fica pendente
        try (var silentBroker = new ServerSocket(0)) {
            var adapter = new MqttTelemetryAdapter("tcp://localhost:" + silentBroker.getLocalPort(), "marazul/vehicles/+/telemetry", prefix,
                    new TelemetryMessageParser(JsonMapper.builder().build()));

            adapter.start();
            adapter.stop();

            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(Thread.getAllStackTraces().keySet())
                    .noneMatch(thread -> thread.getName().contains(prefix)));
        }
    }
}
