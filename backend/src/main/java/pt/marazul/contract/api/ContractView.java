package pt.marazul.contract.api;

import pt.marazul.contract.domain.ContractTerms;
import pt.marazul.contract.domain.ContractVersion;

import java.time.Duration;
import java.time.Instant;

public record ContractView(
        int version,
        Instant effectiveFrom,
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

    static ContractView from(ContractVersion version) {
        ContractTerms terms = version.terms();
        return new ContractView(version.number(), version.effectiveFrom(), terms.unlockFeeCents(),
                terms.perMinuteCents(), terms.depositCents(), terms.surchargePerMinuteCents(),
                terms.surchargeToleranceMinutes(), terms.surchargeCapCents(), terms.slaCriticalHours(),
                terms.slaHighHours(), terms.slaNormalHours(), terms.minimumBatteryPercentage(),
                terms.pickupWindowMinutes());
    }

    public Duration pickupWindow() {
        return Duration.ofMinutes(pickupWindowMinutes);
    }
}
