package pt.marazul.shared.domain;

import java.util.Objects;

public class BusinessRuleViolation extends RuntimeException {
    private final String rule;

    public BusinessRuleViolation(String rule, String message) {
        super(message);
        this.rule = Objects.requireNonNull(rule, "rule");
    }

    public String rule() {
        return rule;
    }
}
