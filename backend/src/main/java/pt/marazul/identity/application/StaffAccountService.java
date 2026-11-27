package pt.marazul.identity.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pt.marazul.identity.domain.StaffAccount;
import pt.marazul.identity.domain.StaffRole;

import java.util.Locale;
import java.util.UUID;

@Service
public class StaffAccountService {
    private final StaffAccountRepository accounts;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(StaffAccountRepository accounts, PasswordEncoder passwordEncoder) {
        this.accounts = accounts;
        this.passwordEncoder = passwordEncoder;
    }

    public StaffAccount create(String email, String displayName, String password, StaffRole role) {
        String normalizedEmail = normalizeEmail(email);
        if (accounts.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateStaffEmailException(normalizedEmail);
        }
        if (password == null || password.length() < 12) {
            throw new InvalidStaffAccountException("A palavra-passe deve ter pelo menos 12 caracteres.");
        }
        StaffAccount account = new StaffAccount(
                UUID.randomUUID(), normalizedEmail, displayName.trim(), passwordEncoder.encode(password), role, true);
        return accounts.save(account);
    }

    public StaffAccount authenticate(String email, String password) {
        StaffAccount account = accounts.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        if (!account.active() || !passwordEncoder.matches(password, account.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return account;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static class DuplicateStaffEmailException extends RuntimeException {
        public DuplicateStaffEmailException(String email) {
            super("Já existe uma conta para o email " + email + ".");
        }
    }

    public static class InvalidStaffAccountException extends RuntimeException {
        public InvalidStaffAccountException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Credenciais inválidas.");
        }
    }
}
