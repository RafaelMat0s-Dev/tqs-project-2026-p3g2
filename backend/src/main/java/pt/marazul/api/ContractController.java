package pt.marazul.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contract")
public class ContractController {
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CLIENT_MANAGER')")
    public void updateContract() {
        // Contract management is implemented by the contract module.
    }
}
