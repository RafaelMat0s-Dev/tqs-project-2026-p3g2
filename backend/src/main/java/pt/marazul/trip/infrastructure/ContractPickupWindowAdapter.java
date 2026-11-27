package pt.marazul.trip.infrastructure;

import org.springframework.stereotype.Component;
import pt.marazul.contract.api.ContractFacade;
import pt.marazul.trip.application.PickupWindowPort;

import java.time.Duration;

@Component
public class ContractPickupWindowAdapter implements PickupWindowPort {
    private final ContractFacade contract;

    public ContractPickupWindowAdapter(ContractFacade contract) {
        this.contract = contract;
    }

    @Override
    public Duration pickupWindow() {
        return contract.pickupWindow();
    }
}
