package pt.marazul.support;

import pt.marazul.contract.api.ContractFacade;
import pt.marazul.contract.api.ContractView;

import java.time.Instant;
import java.util.Optional;

public class FixedContractFacade implements ContractFacade {
    private final ContractView view;

    public FixedContractFacade() {
        this(ContractFixtures.v1Terms().minimumBatteryPercentage());
    }

    public FixedContractFacade(int minimumBatteryPercentage) {
        this.view = new ContractView(1, ContractFixtures.V1_EFFECTIVE_FROM, 100, 5, 2500, 10, 5, 2500, 4, 24, 72,
                minimumBatteryPercentage, 15);
    }

    @Override
    public ContractView current() {
        return view;
    }

    @Override
    public ContractView inForceAt(Instant instant) {
        return view;
    }

    @Override
    public Optional<ContractView> version(int number) {
        return number == view.version() ? Optional.of(view) : Optional.empty();
    }
}
