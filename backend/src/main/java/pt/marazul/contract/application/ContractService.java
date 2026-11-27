package pt.marazul.contract.application;

import org.springframework.stereotype.Service;
import pt.marazul.contract.domain.ContractHistory;
import pt.marazul.contract.domain.ContractTerms;
import pt.marazul.contract.domain.ContractVersion;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class ContractService {
    private final ContractVersionRepository versions;
    private final Clock clock;

    public ContractService(ContractVersionRepository versions, Clock clock) {
        this.versions = versions;
        this.clock = clock;
    }

    public ContractVersion current() {
        return inForceAt(clock.instant());
    }

    public ContractVersion inForceAt(Instant instant) {
        return history().inForceAt(instant)
                .orElseThrow(() -> new IllegalStateException("Não há versão do contrato em vigor em " + instant + "."));
    }

    public Optional<ContractVersion> version(int number) {
        return history().byNumber(number);
    }

    public ContractVersion publish(ContractTerms terms, Instant effectiveFrom) {
        ContractVersion version = history().publish(terms, effectiveFrom == null ? clock.instant().truncatedTo(ChronoUnit.MILLIS) : effectiveFrom);
        versions.save(version);
        return version;
    }

    private ContractHistory history() {
        return new ContractHistory(versions.findAll());
    }
}
