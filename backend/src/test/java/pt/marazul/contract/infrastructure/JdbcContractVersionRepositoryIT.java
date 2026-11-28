package pt.marazul.contract.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import pt.marazul.contract.api.ContractFacade;
import pt.marazul.contract.application.ContractService;
import pt.marazul.contract.application.ContractVersionRepository;
import pt.marazul.contract.domain.ContractVersion;
import pt.marazul.shared.domain.BusinessRuleConflict;
import pt.marazul.support.ContractFixtures;
import pt.marazul.support.TestcontainersConfiguration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JdbcContractVersionRepositoryIT {
    @Autowired
    ContractVersionRepository repository;

    @Autowired
    ContractService service;

    @Autowired
    ContractFacade facade;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void migrationSeedsVersion1WithTheAgreedValues() {
        assertThat(repository.findAll()).first().isEqualTo(ContractFixtures.v1());
        assertThat(facade.version(1)).get().satisfies(view -> {
            assertThat(view.unlockFeeCents()).isEqualTo(100);
            assertThat(view.perMinuteCents()).isEqualTo(5);
            assertThat(view.depositCents()).isEqualTo(2500);
            assertThat(view.surchargePerMinuteCents()).isEqualTo(10);
            assertThat(view.surchargeToleranceMinutes()).isEqualTo(5);
            assertThat(view.surchargeCapCents()).isEqualTo(2500);
            assertThat(view.slaCriticalHours()).isEqualTo(4);
            assertThat(view.slaHighHours()).isEqualTo(24);
            assertThat(view.slaNormalHours()).isEqualTo(72);
            assertThat(view.minimumBatteryPercentage()).isEqualTo(25);
            assertThat(view.pickupWindowMinutes()).isEqualTo(15);
        });
    }

    @Test
    void publishingANewVersionPersistsItAndLeavesVersion1Untouched() {
        ContractVersion published = service.publish(ContractFixtures.terms().perMinuteCents(7).build(), null);

        assertThat(published.number()).isGreaterThanOrEqualTo(2);
        assertThat(repository.findAll()).contains(published);
        assertThat(facade.version(published.number())).get().extracting(view -> view.perMinuteCents()).isEqualTo(7);
        assertThat(facade.version(1)).get().extracting(view -> view.perMinuteCents()).isEqualTo(5);
        assertThat(facade.inForceAt(ContractFixtures.V1_EFFECTIVE_FROM).version()).isEqualTo(1);
        assertThat(facade.inForceAt(published.effectiveFrom()).version()).isEqualTo(published.number());
    }

    @Test
    void aVersionNumberCannotBePublishedTwice() {
        ContractVersion existing = repository.findAll().getFirst();

        assertThatThrownBy(() -> repository.save(new ContractVersion(existing.number(),
                Instant.now().plus(7300, ChronoUnit.DAYS), ContractFixtures.v1Terms())))
                .isInstanceOfSatisfying(BusinessRuleConflict.class, error -> assertThat(error.rule()).isEqualTo("BR10"));
    }

    @Test
    void publishedVersionsCannotBeUpdatedOrDeleted() {
        assertThatThrownBy(() -> jdbc.update("UPDATE contract_version SET per_minute_cents = 99 WHERE version = 1"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("imutáveis");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM contract_version WHERE version = 1"))
                .isInstanceOf(DataAccessException.class).hasMessageContaining("imutáveis");
        assertThat(facade.version(1)).get().extracting(view -> view.perMinuteCents()).isEqualTo(5);
    }

    @Test
    void theDatabaseRefusesNegativeMoney() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO contract_version VALUES
                (999, now(), -1, 5, 2500, 10, 5, 2500, 4, 24, 72, 25, 15)"""))
                .isInstanceOf(DataAccessException.class);
    }
}
