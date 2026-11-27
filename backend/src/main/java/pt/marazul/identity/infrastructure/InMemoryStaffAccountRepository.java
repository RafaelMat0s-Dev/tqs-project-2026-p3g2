package pt.marazul.identity.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.identity.application.StaffAccountRepository;
import pt.marazul.identity.domain.StaffAccount;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Repository
public class InMemoryStaffAccountRepository implements StaffAccountRepository {
    private final ConcurrentMap<String, StaffAccount> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<StaffAccount> findByEmail(String email) {
        return Optional.ofNullable(accounts.get(email));
    }

    @Override
    public StaffAccount save(StaffAccount account) {
        StaffAccount previous = accounts.putIfAbsent(account.email(), account);
        if (previous != null) {
            throw new IllegalStateException("A conta já existe.");
        }
        return account;
    }
}
