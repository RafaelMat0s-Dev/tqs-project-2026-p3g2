package pt.marazul.fleet.domain;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import pt.marazul.shared.domain.BusinessRuleViolation;

public record Vehicle(UUID id, String code, String type, String baseHubCode, VehicleStatus status,
        Integer batteryPercentage, Instant lastReadingAt) {

    public static final String RULE = "VEHICLE_LIFECYCLE";

    private static final Pattern CODE = Pattern.compile("B-\\d{2,3}");

    public static Vehicle register(UUID id, String code, String type, String baseHubCode) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Código inválido: esperado B-NN ou B-NNN");
        }
        requireText(type, 32, "tipo");
        requireText(baseHubCode, 64, "hub de base");
        return new Vehicle(id, code, type, baseHubCode, VehicleStatus.DISABLED, null, null);
    }

    public Vehicle moveTo(VehicleStatus target) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleViolation(RULE,
                    "Transição inválida: " + status.label() + " → " + target.label());
        }
        return new Vehicle(id, code, type, baseHubCode, target, batteryPercentage, lastReadingAt);
    }

    public Vehicle withBattery(int percentage, Instant readingAt) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Bateria fora do intervalo 0 a 100");
        }
        if (lastReadingAt != null && !readingAt.isAfter(lastReadingAt)) {
            return this;
        }
        return new Vehicle(id, code, type, baseHubCode, status, percentage, readingAt);
    }

    private static void requireText(String value, int max, String name) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException("Valor inválido para " + name);
        }
    }
}
