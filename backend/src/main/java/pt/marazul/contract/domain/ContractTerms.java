package pt.marazul.contract.domain;

import pt.marazul.shared.domain.BusinessRuleViolation;

import java.time.Duration;

public record ContractTerms(
        int unlockFeeCents,
        int perMinuteCents,
        int depositCents,
        int surchargePerMinuteCents,
        int surchargeToleranceMinutes,
        int surchargeCapCents,
        int slaCriticalHours,
        int slaHighHours,
        int slaNormalHours,
        int minimumBatteryPercentage,
        int pickupWindowMinutes) {

    public ContractTerms {
        require(unlockFeeCents >= 0, "BR10", "O preço de desbloqueio não pode ser negativo.");
        require(perMinuteCents >= 0, "BR10", "O preço por minuto não pode ser negativo.");
        require(depositCents >= 0, "BR9", "A caução não pode ser negativa.");
        require(surchargePerMinuteCents >= 0, "BR11", "A sobretaxa por minuto não pode ser negativa.");
        require(surchargeToleranceMinutes >= 0, "BR11", "A tolerância da sobretaxa não pode ser negativa.");
        require(surchargeCapCents >= 0, "BR11", "O limite da sobretaxa não pode ser negativo.");
        require(slaCriticalHours >= 1, "BR15", "O prazo crítico tem de ter pelo menos 1 hora de operação.");
        require(slaHighHours >= 1, "BR15", "O prazo alto tem de ter pelo menos 1 hora de operação.");
        require(slaNormalHours >= 1, "BR15", "O prazo normal tem de ter pelo menos 1 hora de operação.");
        require(minimumBatteryPercentage >= 0 && minimumBatteryPercentage <= 100, "BR1",
                "O mínimo de bateria tem de estar entre 0 % e 100 %.");
        require(pickupWindowMinutes >= 1, "BR6", "A janela de levantamento tem de ter pelo menos 1 minuto.");
    }

    public Duration pickupWindow() {
        return Duration.ofMinutes(pickupWindowMinutes);
    }

    private static void require(boolean valid, String rule, String message) {
        if (!valid) {
            throw new BusinessRuleViolation(rule, message);
        }
    }
}
