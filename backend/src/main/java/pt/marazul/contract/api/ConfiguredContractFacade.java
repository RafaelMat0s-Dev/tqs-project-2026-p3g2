package pt.marazul.contract.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ConfiguredContractFacade implements ContractFacade {
    private final Duration pickupWindow;

    public ConfiguredContractFacade(@Value("${marazul.contract.pickup-window:15m}") Duration pickupWindow) {
        this.pickupWindow = pickupWindow;
    }

    @Override
    public Duration pickupWindow() {
        return pickupWindow;
    }
}
