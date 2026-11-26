package pt.marazul.telemetry.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pt.marazul.telemetry.domain.InvalidTelemetryReading;
import pt.marazul.telemetry.domain.TelemetryReading;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryMessageParserTest {
    private static final String TOPIC = "marazul/vehicles/B-17/telemetry";

    private final TelemetryMessageParser parser = new TelemetryMessageParser(JsonMapper.builder().build());

    @Test
    void validJsonBecomesTheExpectedReading() {
        var json = json("B-17", "\"2026-11-20T14:31:07.000Z\"", "47", "\"sao-bras\"");

        assertThat(parser.parse(TOPIC, bytes(json))).isEqualTo(
                new TelemetryReading("B-17", Instant.parse("2026-11-20T14:31:07Z"), 47, true, "sao-bras", false));
    }

    @Test
    void nullHubIsAccepted() {
        var reading = parser.parse(TOPIC, bytes(json("B-17", "\"2026-11-20T14:31:07Z\"", "47", "null")));

        assertThat(reading.hubId()).isNull();
    }

    @ParameterizedTest(name = "bateria {0} é aceite")
    @MethodSource("validBatteries")
    void batteryBoundariesAreAccepted(int battery) {
        var reading = parser.parse(TOPIC, bytes(json("B-17", "\"2026-11-20T14:31:07Z\"", "" + battery, "null")));

        assertThat(reading.batteryPercentage()).isEqualTo(battery);
    }

    static Stream<Integer> validBatteries() {
        return Stream.of(0, 100);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMessages")
    void invalidMessageIsRejectedWithAReason(String description, String topic, byte[] payload) {
        assertThatThrownBy(() -> parser.parse(topic, payload))
                .isInstanceOf(InvalidTelemetryReading.class)
                .satisfies(e -> assertThat(e.getMessage()).isNotBlank());
    }

    static Stream<Arguments> invalidMessages() {
        var ok = "\"2026-11-20T14:31:07Z\"";
        return Stream.of(
                Arguments.of("JSON inválido", TOPIC, bytes("{not json")),
                Arguments.of("payload vazio", TOPIC, new byte[0]),
                Arguments.of("bytes não UTF-8", TOPIC, new byte[]{(byte) 0xC3, 0x28, (byte) 0xFF}),
                Arguments.of("campo em falta (bateria)", TOPIC,
                        bytes("{\"vehicleId\":\"B-17\",\"originAt\":" + ok + ",\"locked\":true,\"shock\":false}")),
                Arguments.of("campo em falta (originAt)", TOPIC,
                        bytes("{\"vehicleId\":\"B-17\",\"batteryPercentage\":47,\"locked\":true,\"shock\":false}")),
                Arguments.of("bateria 101", TOPIC, bytes(json("B-17", ok, "101", "null"))),
                Arguments.of("bateria -1", TOPIC, bytes(json("B-17", ok, "-1", "null"))),
                Arguments.of("vehicleId diferente do tópico", TOPIC, bytes(json("B-18", ok, "47", "null"))),
                Arguments.of("originAt mal formado", TOPIC, bytes(json("B-17", "\"ontem à tarde\"", "47", "null"))));
    }

    private static String json(String vehicleId, String originAt, String battery, String hub) {
        return "{\"vehicleId\":\"" + vehicleId + "\",\"originAt\":" + originAt + ",\"batteryPercentage\":" + battery
                + ",\"locked\":true,\"hubId\":" + hub + ",\"shock\":false}";
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
