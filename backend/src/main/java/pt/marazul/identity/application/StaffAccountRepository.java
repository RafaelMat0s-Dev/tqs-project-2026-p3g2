package pt.marazul.identity.application;

import pt.marazul.identity.domain.StaffAccount;

import java.util.Optional;

public interface StaffAccountRepository {
    Optional<StaffAccount> findByEmail(String email);
    StaffAccount save(StaffAccount account);
}
