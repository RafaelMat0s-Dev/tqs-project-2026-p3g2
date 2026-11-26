package pt.marazul.telemetry.infrastructure;

import org.springframework.stereotype.Component;
import pt.marazul.telemetry.domain.InvalidTelemetryReading;
import pt.marazul.telemetry.domain.TelemetryReading;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;

@Component
class TelemetryMessageParser {
    private final JsonMapper mapper;

    TelemetryMessageParser(JsonMapper mapper) {
        this.mapper = mapper;
    }

    TelemetryReading parse(String topic, byte[] payload) {
        var root = readTree(decode(payload));
        if (!root.isObject()) {
            throw new InvalidTelemetryReading("A mensagem de telemetria tem de ser um objeto JSON.");
        }
        var vehicleId = text(root, "vehicleId");
        var topicVehicle = topic.split("/").length > 2 ? topic.split("/")[2] : "";
        if (!vehicleId.equals(topicVehicle)) {
            throw new InvalidTelemetryReading("O vehicleId da mensagem não corresponde ao do tópico.");
        }
        var battery = root.get("batteryPercentage");
        if (battery == null || !battery.isIntegralNumber() || !battery.canConvertToInt()) {
            throw new InvalidTelemetryReading("O campo batteryPercentage é obrigatório e tem de ser inteiro.");
        }
        var hub = root.get("hubId");
        return new TelemetryReading(vehicleId, instant(text(root, "originAt")), battery.intValue(),
                bool(root, "locked"), hub == null || hub.isNull() ? null : hub.asString(), bool(root, "shock"));
    }

    private static String decode(byte[] payload) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(payload)).toString();
        } catch (CharacterCodingException e) {
            throw new InvalidTelemetryReading("A mensagem de telemetria não está em UTF-8.");
        }
    }

    private JsonNode readTree(String text) {
        try {
            return mapper.readTree(text);
        } catch (JacksonException e) {
            throw new InvalidTelemetryReading("A mensagem de telemetria não é JSON válido.");
        }
    }

    private static String text(JsonNode root, String field) {
        var node = root.get(field);
        if (node == null || !node.isString()) {
            throw new InvalidTelemetryReading("O campo " + field + " é obrigatório.");
        }
        return node.asString();
    }

    private static boolean bool(JsonNode root, String field) {
        var node = root.get(field);
        if (node == null || !node.isBoolean()) {
            throw new InvalidTelemetryReading("O campo " + field + " é obrigatório e tem de ser booleano.");
        }
        return node.booleanValue();
    }

    private static Instant instant(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidTelemetryReading("O campo originAt não é um instante ISO-8601 válido.");
        }
    }
}
