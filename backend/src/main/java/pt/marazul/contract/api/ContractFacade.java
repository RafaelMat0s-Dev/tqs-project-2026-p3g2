package pt.marazul.contract.api;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface ContractFacade {
    ContractView current();

    ContractView inForceAt(Instant instant);

    Optional<ContractView> version(int number);

    default Duration pickupWindow() {
        return current().pickupWindow();
    }
}
