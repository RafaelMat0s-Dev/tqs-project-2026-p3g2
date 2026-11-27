package pt.marazul.booking.infrastructure;

import org.springframework.stereotype.Component;
import pt.marazul.booking.application.MinimumBatteryPort;
import pt.marazul.contract.api.ContractFacade;

@Component
public class ContractMinimumBatteryAdapter implements MinimumBatteryPort {
    private final ContractFacade contract;

    public ContractMinimumBatteryAdapter(ContractFacade contract) {
        this.contract = contract;
    }

    @Override
    public int minimumBatteryPercentage() {
        return contract.current().minimumBatteryPercentage();
    }
}
