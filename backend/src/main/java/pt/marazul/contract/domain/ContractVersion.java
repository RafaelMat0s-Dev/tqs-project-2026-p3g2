package pt.marazul.contract.domain;

import java.time.Instant;
import java.util.Objects;

public record ContractVersion(int number, Instant effectiveFrom, ContractTerms terms) {
    public ContractVersion {
        if (number < 1) {
            throw new IllegalArgumentException("O número da versão tem de ser positivo.");
        }
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(terms, "terms");
    }
}
