package pt.marazul.contract.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.marazul.contract.application.ContractService;
import pt.marazul.contract.domain.ContractTerms;

import java.time.Instant;

@RestController
@RequestMapping("/api/contract")
public class ContractController {
    private static final String STAFF_READERS = "hasAnyRole('ADMINISTRATOR', 'COORDINATOR', 'CLIENT_MANAGER')";

    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(STAFF_READERS)
    public ContractView current() {
        return ContractView.from(service.current());
    }

    @GetMapping("/versions/{number}")
    @PreAuthorize(STAFF_READERS)
    public ContractView version(@PathVariable int number) {
        return service.version(number).map(ContractView::from).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Não existe a versão " + number + " do contrato."));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ContractView publish(@Valid @RequestBody PublishContractRequest request) {
        return ContractView.from(service.publish(request.toTerms(), request.effectiveFrom()));
    }

    public record PublishContractRequest(
            Instant effectiveFrom,
            @NotNull Integer unlockFeeCents,
            @NotNull Integer perMinuteCents,
            @NotNull Integer depositCents,
            @NotNull Integer surchargePerMinuteCents,
            @NotNull Integer surchargeToleranceMinutes,
            @NotNull Integer surchargeCapCents,
            @NotNull Integer slaCriticalHours,
            @NotNull Integer slaHighHours,
            @NotNull Integer slaNormalHours,
            @NotNull Integer minimumBatteryPercentage,
            @NotNull Integer pickupWindowMinutes) {

        ContractTerms toTerms() {
            return new ContractTerms(unlockFeeCents, perMinuteCents, depositCents, surchargePerMinuteCents,
                    surchargeToleranceMinutes, surchargeCapCents, slaCriticalHours, slaHighHours, slaNormalHours,
                    minimumBatteryPercentage, pickupWindowMinutes);
        }
    }
}
