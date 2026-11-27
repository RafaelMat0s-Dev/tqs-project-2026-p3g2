package pt.marazul.contract.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ContractHistory {
    private final List<ContractVersion> versions;

    public ContractHistory(List<ContractVersion> versions) {
        List<ContractVersion> sorted = versions.stream().sorted(Comparator.comparingInt(ContractVersion::number)).toList();
        for (int index = 0; index < sorted.size(); index++) {
            if (sorted.get(index).number() != index + 1) {
                throw new IllegalArgumentException("As versões do contrato têm de ser numeradas sem falhas a partir de 1.");
            }
            if (index > 0 && !sorted.get(index).effectiveFrom().isAfter(sorted.get(index - 1).effectiveFrom())) {
                throw new IllegalArgumentException("As datas de entrada em vigor têm de ser crescentes.");
            }
        }
        this.versions = sorted;
    }

    public List<ContractVersion> versions() {
        return versions;
    }

    public Optional<ContractVersion> inForceAt(Instant instant) {
        return versions.stream().filter(version -> !version.effectiveFrom().isAfter(instant))
                .reduce((earlier, later) -> later);
    }

    public Optional<ContractVersion> byNumber(int number) {
        return versions.stream().filter(version -> version.number() == number).findFirst();
    }

    public ContractVersion publish(ContractTerms terms, Instant effectiveFrom) {
        if (!versions.isEmpty() && !effectiveFrom.isAfter(versions.getLast().effectiveFrom())) {
            throw new BusinessRuleViolation("BR10",
                    "A nova versão do contrato tem de entrar em vigor depois da versão " + versions.getLast().number() + ".");
        }
        return new ContractVersion(versions.size() + 1, effectiveFrom, terms);
    }
}
