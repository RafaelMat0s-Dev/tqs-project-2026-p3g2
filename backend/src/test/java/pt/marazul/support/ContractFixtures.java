package pt.marazul.support;

import pt.marazul.contract.domain.ContractTerms;
import pt.marazul.contract.domain.ContractVersion;

import java.time.Instant;

public final class ContractFixtures {
    public static final Instant V1_EFFECTIVE_FROM = Instant.parse("1970-01-01T00:00:00Z");

    private ContractFixtures() {
    }

    public static ContractTerms v1Terms() {
        return terms().build();
    }

    public static ContractVersion v1() {
        return new ContractVersion(1, V1_EFFECTIVE_FROM, v1Terms());
    }

    public static Builder terms() {
        return new Builder();
    }

    public static final class Builder {
        private int unlockFeeCents = 100;
        private int perMinuteCents = 5;
        private int depositCents = 2500;
        private int surchargePerMinuteCents = 10;
        private int surchargeToleranceMinutes = 5;
        private int surchargeCapCents = 2500;
        private int slaCriticalHours = 4;
        private int slaHighHours = 24;
        private int slaNormalHours = 72;
        private int minimumBatteryPercentage = 25;
        private int pickupWindowMinutes = 15;

        public Builder unlockFeeCents(int value) {
            unlockFeeCents = value;
            return this;
        }

        public Builder perMinuteCents(int value) {
            perMinuteCents = value;
            return this;
        }

        public Builder depositCents(int value) {
            depositCents = value;
            return this;
        }

        public Builder surchargePerMinuteCents(int value) {
            surchargePerMinuteCents = value;
            return this;
        }

        public Builder surchargeToleranceMinutes(int value) {
            surchargeToleranceMinutes = value;
            return this;
        }

        public Builder surchargeCapCents(int value) {
            surchargeCapCents = value;
            return this;
        }

        public Builder slaCriticalHours(int value) {
            slaCriticalHours = value;
            return this;
        }

        public Builder slaHighHours(int value) {
            slaHighHours = value;
            return this;
        }

        public Builder slaNormalHours(int value) {
            slaNormalHours = value;
            return this;
        }

        public Builder minimumBatteryPercentage(int value) {
            minimumBatteryPercentage = value;
            return this;
        }

        public Builder pickupWindowMinutes(int value) {
            pickupWindowMinutes = value;
            return this;
        }

        public ContractTerms build() {
            return new ContractTerms(unlockFeeCents, perMinuteCents, depositCents, surchargePerMinuteCents,
                    surchargeToleranceMinutes, surchargeCapCents, slaCriticalHours, slaHighHours, slaNormalHours,
                    minimumBatteryPercentage, pickupWindowMinutes);
        }
    }
}
