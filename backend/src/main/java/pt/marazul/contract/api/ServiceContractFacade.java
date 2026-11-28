package pt.marazul.contract.api;

import org.springframework.stereotype.Component;
import pt.marazul.contract.application.ContractService;

import java.time.Instant;
import java.util.Optional;

@Component
public class ServiceContractFacade implements ContractFacade {
    private final ContractService service;

    public ServiceContractFacade(ContractService service) {
        this.service = service;
    }

    @Override
    public ContractView current() {
        return ContractView.from(service.current());
    }

    @Override
    public ContractView inForceAt(Instant instant) {
        return ContractView.from(service.inForceAt(instant));
    }

    @Override
    public Optional<ContractView> version(int number) {
        return service.version(number).map(ContractView::from);
    }
}
