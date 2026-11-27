package pt.marazul.contract.api;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredContractFacadeTest {
    @Test
    void exposesTheConfiguredPickupWindow() {
        assertThat(new ConfiguredContractFacade(Duration.ofMinutes(15)).pickupWindow()).isEqualTo(Duration.ofMinutes(15));
    }
}
