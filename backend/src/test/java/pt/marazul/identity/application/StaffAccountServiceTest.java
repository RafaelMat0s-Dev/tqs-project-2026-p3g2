package pt.marazul.identity.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import pt.marazul.identity.domain.StaffAccount;
import pt.marazul.identity.domain.StaffRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffAccountServiceTest {
    private InMemoryAccounts accounts;
    private StaffAccountService service;

    @BeforeEach
    void setUp() {
        accounts = new InMemoryAccounts();
        service = new StaffAccountService(accounts, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    @Test
    void createsAccountWithArgon2HashAndNormalizedEmail() {
        StaffAccount account = service.create(" NIDIA@MARAZUL.TEST ", "Nídia Tavares",
                "uma-palavra-passe-segura", StaffRole.TECHNICIAN);

        assertThat(account.email()).isEqualTo("nidia@marazul.test");
        assertThat(account.passwordHash()).startsWith("$argon2");
        assertThat(service.authenticate("nidia@marazul.test", "uma-palavra-passe-segura")).isEqualTo(account);
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingAnotherAccount() {
        service.create("nidia@marazul.test", "Nídia", "uma-palavra-passe-segura", StaffRole.TECHNICIAN);

        assertThatThrownBy(() -> service.create("NIDIA@MARAZUL.TEST", "Outra Nídia",
                "outra-palavra-passe", StaffRole.COORDINATOR))
                .isInstanceOf(StaffAccountService.DuplicateStaffEmailException.class);
        assertThat(accounts.findByEmail("nidia@marazul.test")).get().extracting(StaffAccount::displayName)
                .isEqualTo("Nídia");
    }

    @Test
    void rejectsDisabledAccountEvenWithCorrectPassword() {
        StaffAccount account = service.create("nidia@marazul.test", "Nídia",
                "uma-palavra-passe-segura", StaffRole.TECHNICIAN);
        accounts.replace(account.deactivate());

        assertThatThrownBy(() -> service.authenticate("nidia@marazul.test", "uma-palavra-passe-segura"))
                .isInstanceOf(StaffAccountService.InvalidCredentialsException.class);
    }

    private static final class InMemoryAccounts implements StaffAccountRepository {
        private StaffAccount account;

        @Override
        public Optional<StaffAccount> findByEmail(String email) {
            return Optional.ofNullable(account).filter(value -> value.email().equals(email));
        }

        @Override
        public StaffAccount save(StaffAccount value) {
            if (account != null) {
                throw new IllegalStateException("duplicate");
            }
            account = value;
            return value;
        }

        void replace(StaffAccount value) {
            account = value;
        }
    }
}
