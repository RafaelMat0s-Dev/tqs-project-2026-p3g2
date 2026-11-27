package pt.marazul.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pt.marazul.fleet.domain.VehicleStatus.AVAILABLE;
import static pt.marazul.fleet.domain.VehicleStatus.DISABLED;
import static pt.marazul.fleet.domain.VehicleStatus.IN_USE;
import static pt.marazul.fleet.domain.VehicleStatus.MAINTENANCE;
import static pt.marazul.fleet.domain.VehicleStatus.RESERVED;
import static pt.marazul.fleet.domain.VehicleStatus.RETIRED;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pt.marazul.shared.domain.BusinessRuleViolation;

class VehicleTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZoneOffset.UTC);
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000017");

    // Figura 2, decisão D1: os 13 pares válidos, escritos à mão.
    private static final List<List<VehicleStatus>> VALID = List.of(
            List.of(DISABLED, AVAILABLE), List.of(AVAILABLE, DISABLED), List.of(DISABLED, RETIRED),
            List.of(MAINTENANCE, RETIRED), List.of(AVAILABLE, RESERVED), List.of(RESERVED, AVAILABLE),
            List.of(RESERVED, IN_USE), List.of(IN_USE, AVAILABLE), List.of(AVAILABLE, MAINTENANCE),
            List.of(RESERVED, MAINTENANCE), List.of(DISABLED, MAINTENANCE), List.of(IN_USE, MAINTENANCE),
            List.of(MAINTENANCE, AVAILABLE));

    private static Stream<Arguments> allPairs() {
        return Stream.of(VehicleStatus.values()).flatMap(from -> Stream.of(VehicleStatus.values())
                .map(to -> Arguments.of(from, to, VALID.contains(List.of(from, to)))));
    }

    private static Vehicle vehicleIn(VehicleStatus status) {
        return new Vehicle(ID, "B-17", "bike", "Hotel Alvina Mar", status, null, null);
    }

    private static Vehicle fresh() {
        return Vehicle.register(ID, "B-17", "bike", "Hotel Alvina Mar");
    }

    @Test
    void figureTwoHasExactlyThirteenValidPairsOutOfThirtySix() {
        assertThat(allPairs().count()).isEqualTo(36);
        assertThat(VALID).hasSize(13).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0} -> {1} valid={2}")
    @MethodSource("allPairs")
    void canMoveToMatchesFigureTwoForEveryPair(VehicleStatus from, VehicleStatus to, boolean valid) {
        assertThat(from.canMoveTo(to)).isEqualTo(valid);
    }

    @ParameterizedTest(name = "{0} -> {1} valid={2}")
    @MethodSource("allPairs")
    void moveToAppliesValidTransitionsAndRejectsTheRest(VehicleStatus from, VehicleStatus to, boolean valid) {
        Vehicle vehicle = vehicleIn(from);
        if (valid) {
            Vehicle moved = vehicle.moveTo(to);
            assertThat(moved.status()).isEqualTo(to);
            assertThat(moved.id()).isEqualTo(ID);
            assertThat(moved.code()).isEqualTo("B-17");
        } else {
            assertThatThrownBy(() -> vehicle.moveTo(to))
                    .isInstanceOfSatisfying(BusinessRuleViolation.class, e -> {
                        assertThat(e.rule()).isEqualTo("VEHICLE_LIFECYCLE");
                        assertThat(e.getMessage())
                                .isEqualTo("Transição inválida: " + from.label() + " → " + to.label());
                    });
        }
    }

    @Test
    void rejectsTransitionNotInFigure2WithTheExactMessage() {
        assertThatThrownBy(() -> vehicleIn(RETIRED).moveTo(AVAILABLE))
                .isInstanceOfSatisfying(BusinessRuleViolation.class, e -> {
                    assertThat(e.rule()).isEqualTo("VEHICLE_LIFECYCLE");
                    assertThat(e.getMessage()).isEqualTo("Transição inválida: Abatida → Disponível");
                });
    }

    @Test
    void labelsArePortuguese() {
        assertThat(VehicleStatus.values()).extracting(VehicleStatus::label)
                .containsExactly("Inativa", "Disponível", "Reservada", "Em uso", "Manutenção", "Abatida");
    }

    @ParameterizedTest
    @EnumSource(VehicleStatus.class)
    void retiredIsTerminal(VehicleStatus target) {
        assertThat(RETIRED.canMoveTo(target)).isFalse();
        assertThatThrownBy(() -> vehicleIn(RETIRED).moveTo(target)).isInstanceOf(BusinessRuleViolation.class);
    }

    @Test
    void moveToDoesNotMutateTheOriginal() {
        Vehicle original = vehicleIn(AVAILABLE);
        original.moveTo(RESERVED);
        assertThat(original.status()).isEqualTo(AVAILABLE);
    }

    @Test
    void registerStartsDisabledWithoutBatteryNorReading() {
        Vehicle v = fresh();
        assertThat(v.status()).isEqualTo(DISABLED);
        assertThat(v.batteryPercentage()).isNull();
        assertThat(v.lastReadingAt()).isNull();
        assertThat(v.id()).isEqualTo(ID);
        assertThat(v.code()).isEqualTo("B-17");
        assertThat(v.type()).isEqualTo("bike");
        assertThat(v.baseHubCode()).isEqualTo("Hotel Alvina Mar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"B-17", "B-100", "B-99", "B-10"})
    void registerAcceptsCodesOfTwoOrThreeDigits(String code) {
        assertThat(Vehicle.register(ID, code, "bike", "Hotel Alvina Mar").code()).isEqualTo(code);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"b-17", "B-1", "B-1000", "X-17", "B17", "B-1a", " B-17", "B-17 "})
    void registerRejectsMalformedCode(String code) {
        assertThatThrownBy(() -> Vehicle.register(ID, code, "bike", "Hotel Alvina Mar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void registerRejectsBlankType(String type) {
        assertThatThrownBy(() -> Vehicle.register(ID, "B-17", type, "Hotel Alvina Mar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 32})
    void registerAcceptsTypeUpToThirtyTwoCharacters(int length) {
        assertThat(Vehicle.register(ID, "B-17", "t".repeat(length), "Hotel Alvina Mar").type()).hasSize(length);
    }

    @Test
    void registerRejectsTypeOfThirtyThreeCharacters() {
        assertThatThrownBy(() -> Vehicle.register(ID, "B-17", "t".repeat(33), "Hotel Alvina Mar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void registerRejectsBlankBaseHubCode(String hub) {
        assertThatThrownBy(() -> Vehicle.register(ID, "B-17", "bike", hub))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 64})
    void registerAcceptsBaseHubCodeUpToSixtyFourCharacters(int length) {
        assertThat(Vehicle.register(ID, "B-17", "bike", "h".repeat(length)).baseHubCode()).hasSize(length);
    }

    @Test
    void registerRejectsBaseHubCodeOfSixtyFiveCharacters() {
        assertThatThrownBy(() -> Vehicle.register(ID, "B-17", "bike", "h".repeat(65)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withBatteryAppliesFirstReadingWhenThereIsNone() {
        Instant at = CLOCK.instant();
        Vehicle v = fresh().withBattery(80, at);
        assertThat(v.batteryPercentage()).isEqualTo(80);
        assertThat(v.lastReadingAt()).isEqualTo(at);
    }

    @Test
    void withBatteryAppliesStrictlyNewerReading() {
        Vehicle first = fresh().withBattery(80, CLOCK.instant());
        Instant later = CLOCK.instant().plusSeconds(1);
        Vehicle v = first.withBattery(79, later);
        assertThat(v.batteryPercentage()).isEqualTo(79);
        assertThat(v.lastReadingAt()).isEqualTo(later);
    }

    @ParameterizedTest(name = "reading {0}s from the last one is ignored")
    @ValueSource(longs = {0, -1, -3600})
    void withBatteryIgnoresEqualOrOlderReading(long offsetSeconds) {
        Vehicle first = fresh().withBattery(80, CLOCK.instant());
        Vehicle v = first.withBattery(10, CLOCK.instant().plus(Duration.ofSeconds(offsetSeconds)));
        assertThat(v).isSameAs(first);
        assertThat(v.batteryPercentage()).isEqualTo(80);
        assertThat(v.lastReadingAt()).isEqualTo(CLOCK.instant());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 99, 100})
    void withBatteryAcceptsPercentagesFromZeroToOneHundred(int percentage) {
        assertThat(fresh().withBattery(percentage, CLOCK.instant()).batteryPercentage()).isEqualTo(percentage);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 101})
    void withBatteryRejectsPercentagesOutsideZeroToOneHundred(int percentage) {
        assertThatThrownBy(() -> fresh().withBattery(percentage, CLOCK.instant()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
