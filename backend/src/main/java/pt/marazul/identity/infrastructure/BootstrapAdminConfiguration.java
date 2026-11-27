package pt.marazul.identity.infrastructure;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffRole;

@Configuration
public class BootstrapAdminConfiguration {
    private final StaffAccountService accounts;
    private final String email;
    private final String password;

    public BootstrapAdminConfiguration(
            StaffAccountService accounts,
            @Value("${marazul.security.bootstrap-admin-email:}") String email,
            @Value("${marazul.security.bootstrap-admin-password:}") String password) {
        this.accounts = accounts;
        this.email = email;
        this.password = password;
    }

    @PostConstruct
    void bootstrap() {
        if (!email.isBlank() && !password.isBlank()) {
            accounts.create(email, "Administrador Mar Azul", password, StaffRole.ADMINISTRATOR);
        }
    }
}
