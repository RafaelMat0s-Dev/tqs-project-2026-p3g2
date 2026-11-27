package pt.marazul.contract.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.marazul.shared.domain.BusinessRuleViolation;
import pt.marazul.support.ContractFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractHistoryTest {
    private static final Instant V2_FROM = Instant.parse("2026-12-01T09:00:00Z");
    private static final Instant V3_FROM = Instant.parse("2026-12-10T00:00:00Z");

    private final ContractVersion v1 = ContractFixtures.v1();
    private final ContractVersion v2 = new ContractVersion(2, V2_FROM, ContractFixtures.terms().perMinuteCents(7).build());
    private final ContractVersion v3 = new ContractVersion(3, V3_FROM, ContractFixtures.terms().perMinuteCents(9).build());
    private final ContractHistory history = new ContractHistory(List.of(v3, v1, v2));

    @ParameterizedTest(name = "em {0} está em vigor a versão {1}")
    @CsvSource({
            "1970-01-01T00:00:00Z, 1",
            "2026-12-01T08:59:59Z, 1",
            "2026-12-01T09:00:00Z, 2",
            "2026-12-01T09:00:01Z, 2",
            "2026-12-09T23:59:59Z, 2",
            "2026-12-10T00:00:00Z, 3",
            "2030-01-01T00:00:00Z, 3"})
    void picksTheVersionInForceAtTheInstant(String instant, int expected) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);

        assertThat(history.inForceAt(clock.instant())).get().extracting(ContractVersion::number).isEqualTo(expected);
    }

    @Test
    void noVersionIsInForceBeforeTheFirstOne() {
        assertThat(history.inForceAt(Instant.parse("1969-12-31T23:59:59Z"))).isEmpty();
    }

    @Test
    void findsAVersionByItsNumber() {
        assertThat(history.byNumber(2)).contains(v2);
        assertThat(history.byNumber(4)).isEmpty();
        assertThat(history.byNumber(0)).isEmpty();
    }

    @Test
    void publishingAssignsTheNextNumberAndLeavesThePastUntouched() {
        Instant next = Instant.parse("2026-12-20T10:00:00Z");
        ContractTerms terms = ContractFixtures.terms().depositCents(3000).build();

        ContractVersion published = history.publish(terms, next);

        assertThat(published).isEqualTo(new ContractVersion(4, next, terms));
        assertThat(history.versions()).containsExactly(v1, v2, v3);
        assertThat(history.byNumber(1).orElseThrow().terms().perMinuteCents()).isEqualTo(5);
    }

    @Test
    void theFirstPublicationIsVersionOne() {
        ContractVersion published = new ContractHistory(List.of()).publish(ContractFixtures.v1Terms(), V2_FROM);

        assertThat(published.number()).isEqualTo(1);
    }

    @ParameterizedTest(name = "publicar com efeito em {0} viola BR10")
    @CsvSource({"2026-12-10T00:00:00Z", "2026-12-09T23:59:59Z", "1970-01-01T00:00:00Z"})
    void publishingNeedsAnEffectiveDateAfterTheLatestVersion(String effectiveFrom) {
        assertThatThrownBy(() -> history.publish(ContractFixtures.v1Terms(), Instant.parse(effectiveFrom)))
                .isInstanceOfSatisfying(BusinessRuleViolation.class, error -> assertThat(error.rule()).isEqualTo("BR10"));
    }

    @Test
    void publishingJustAfterTheLatestVersionIsAccepted() {
        assertThat(history.publish(ContractFixtures.v1Terms(), V3_FROM.plusSeconds(1)).number()).isEqualTo(4);
    }

    @Test
    void versionsCannotBeChangedThroughTheHistory() {
        assertThatThrownBy(() -> history.versions().add(v1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsGapsInTheNumbering() {
        assertThatThrownBy(() -> new ContractHistory(List.of(v1, v3))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEffectiveDatesThatDoNotIncrease() {
        ContractVersion sameDate = new ContractVersion(2, ContractFixtures.V1_EFFECTIVE_FROM, ContractFixtures.v1Terms());

        assertThatThrownBy(() -> new ContractHistory(List.of(v1, sameDate))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aVersionNeedsAPositiveNumberAnEffectiveDateAndTerms() {
        assertThatThrownBy(() -> new ContractVersion(0, V2_FROM, ContractFixtures.v1Terms()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContractVersion(1, null, ContractFixtures.v1Terms()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ContractVersion(1, V2_FROM, null)).isInstanceOf(NullPointerException.class);
    }
}
