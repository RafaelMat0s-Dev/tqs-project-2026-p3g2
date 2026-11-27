package pt.marazul.contract.application;

import org.junit.jupiter.api.Test;
import pt.marazul.contract.domain.ContractVersion;
import pt.marazul.shared.domain.BusinessRuleViolation;
import pt.marazul.support.ContractFixtures;
import pt.marazul.support.InMemoryContractVersionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractServiceTest {
    private static final Instant NOW = Instant.parse("2026-12-01T09:00:00Z");

    private final InMemoryContractVersionRepository repository = new InMemoryContractVersionRepository();
    private final ContractService service = new ContractService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void theSeededVersionIsInForce() {
        assertThat(service.current()).isEqualTo(ContractFixtures.v1());
    }

    @Test
    void publishingWithoutADateTakesEffectNowAndKeepsTheOldVersion() {
        ContractVersion v2 = service.publish(ContractFixtures.terms().perMinuteCents(7).build(), null);

        assertThat(v2.number()).isEqualTo(2);
        assertThat(v2.effectiveFrom()).isEqualTo(NOW);
        assertThat(service.current()).isEqualTo(v2);
        assertThat(service.version(1)).contains(ContractFixtures.v1());
        assertThat(service.inForceAt(NOW.minusSeconds(1))).isEqualTo(ContractFixtures.v1());
        assertThat(service.inForceAt(NOW)).isEqualTo(v2);
    }

    @Test
    void aFutureVersionOnlyReplacesTheCurrentOneWhenItsTimeComes() {
        Instant later = NOW.plusSeconds(3600);

        ContractVersion v2 = service.publish(ContractFixtures.terms().perMinuteCents(7).build(), later);

        assertThat(service.current().number()).isEqualTo(1);
        assertThat(service.inForceAt(later).number()).isEqualTo(v2.number());
    }

    @Test
    void anUnknownVersionIsEmpty() {
        assertThat(service.version(2)).isEmpty();
    }

    @Test
    void publishingNeedsAnEffectiveDateAfterTheLatestVersion() {
        assertThatThrownBy(() -> service.publish(ContractFixtures.v1Terms(), ContractFixtures.V1_EFFECTIVE_FROM))
                .isInstanceOfSatisfying(BusinessRuleViolation.class, error -> assertThat(error.rule()).isEqualTo("BR10"));
    }

    @Test
    void failsLoudlyWhenNoVersionIsInForce() {
        assertThatThrownBy(() -> service.inForceAt(Instant.parse("1969-12-31T23:59:59Z")))
                .isInstanceOf(IllegalStateException.class);
    }
}
