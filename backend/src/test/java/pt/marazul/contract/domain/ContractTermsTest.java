package pt.marazul.contract.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pt.marazul.shared.domain.BusinessRuleViolation;
import pt.marazul.support.ContractFixtures;
import pt.marazul.support.ContractFixtures.Builder;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractTermsTest {
    @Test
    void v1HoldsTheAgreedValues() {
        ContractTerms terms = ContractFixtures.v1Terms();

        assertThat(terms.unlockFeeCents()).isEqualTo(100);
        assertThat(terms.perMinuteCents()).isEqualTo(5);
        assertThat(terms.depositCents()).isEqualTo(2500);
        assertThat(terms.surchargePerMinuteCents()).isEqualTo(10);
        assertThat(terms.surchargeToleranceMinutes()).isEqualTo(5);
        assertThat(terms.surchargeCapCents()).isEqualTo(2500);
        assertThat(terms.slaCriticalHours()).isEqualTo(4);
        assertThat(terms.slaHighHours()).isEqualTo(24);
        assertThat(terms.slaNormalHours()).isEqualTo(72);
        assertThat(terms.minimumBatteryPercentage()).isEqualTo(25);
        assertThat(terms.pickupWindow()).isEqualTo(Duration.ofMinutes(15));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validBoundaries")
    void acceptsTheBoundaryValue(String description, Consumer<Builder> change) {
        Builder builder = ContractFixtures.terms();
        change.accept(builder);

        assertThatCode(builder::build).doesNotThrowAnyException();
    }

    static Stream<Arguments> validBoundaries() {
        return Stream.of(
                Arguments.of("desbloqueio 0 cêntimos", (Consumer<Builder>) b -> b.unlockFeeCents(0)),
                Arguments.of("minuto 0 cêntimos", (Consumer<Builder>) b -> b.perMinuteCents(0)),
                Arguments.of("caução 0 cêntimos", (Consumer<Builder>) b -> b.depositCents(0)),
                Arguments.of("sobretaxa por minuto 0 cêntimos", (Consumer<Builder>) b -> b.surchargePerMinuteCents(0)),
                Arguments.of("tolerância 0 minutos", (Consumer<Builder>) b -> b.surchargeToleranceMinutes(0)),
                Arguments.of("limite da sobretaxa 0 cêntimos", (Consumer<Builder>) b -> b.surchargeCapCents(0)),
                Arguments.of("prazo crítico 1 hora", (Consumer<Builder>) b -> b.slaCriticalHours(1)),
                Arguments.of("prazo alto 1 hora", (Consumer<Builder>) b -> b.slaHighHours(1)),
                Arguments.of("prazo normal 1 hora", (Consumer<Builder>) b -> b.slaNormalHours(1)),
                Arguments.of("bateria mínima 0 %", (Consumer<Builder>) b -> b.minimumBatteryPercentage(0)),
                Arguments.of("bateria mínima 100 %", (Consumer<Builder>) b -> b.minimumBatteryPercentage(100)),
                Arguments.of("janela de levantamento 1 minuto", (Consumer<Builder>) b -> b.pickupWindowMinutes(1)));
    }

    @ParameterizedTest(name = "{0} viola {2}")
    @MethodSource("invalidValues")
    void rejectsTheValueJustOutsideTheBoundary(String description, Consumer<Builder> change, String rule) {
        Builder builder = ContractFixtures.terms();
        change.accept(builder);

        assertThatThrownBy(builder::build)
                .isInstanceOfSatisfying(BusinessRuleViolation.class, error -> assertThat(error.rule()).isEqualTo(rule));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("desbloqueio -1 cêntimo", (Consumer<Builder>) b -> b.unlockFeeCents(-1), "BR10"),
                Arguments.of("minuto -1 cêntimo", (Consumer<Builder>) b -> b.perMinuteCents(-1), "BR10"),
                Arguments.of("caução -1 cêntimo", (Consumer<Builder>) b -> b.depositCents(-1), "BR9"),
                Arguments.of("sobretaxa por minuto -1 cêntimo", (Consumer<Builder>) b -> b.surchargePerMinuteCents(-1), "BR11"),
                Arguments.of("tolerância -1 minuto", (Consumer<Builder>) b -> b.surchargeToleranceMinutes(-1), "BR11"),
                Arguments.of("limite da sobretaxa -1 cêntimo", (Consumer<Builder>) b -> b.surchargeCapCents(-1), "BR11"),
                Arguments.of("prazo crítico 0 horas", (Consumer<Builder>) b -> b.slaCriticalHours(0), "BR15"),
                Arguments.of("prazo alto 0 horas", (Consumer<Builder>) b -> b.slaHighHours(0), "BR15"),
                Arguments.of("prazo normal 0 horas", (Consumer<Builder>) b -> b.slaNormalHours(0), "BR15"),
                Arguments.of("bateria mínima -1 %", (Consumer<Builder>) b -> b.minimumBatteryPercentage(-1), "BR1"),
                Arguments.of("bateria mínima 101 %", (Consumer<Builder>) b -> b.minimumBatteryPercentage(101), "BR1"),
                Arguments.of("janela de levantamento 0 minutos", (Consumer<Builder>) b -> b.pickupWindowMinutes(0), "BR6"));
    }
}
