package pt.marazul.identity.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.identity.application.GuestDirectory;
import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.Stay;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGuestDirectory implements GuestDirectory {
    private final Map<String, Guest> guestsByCodeHash = new ConcurrentHashMap<>();
    private final Map<UUID, Stay> stays = new ConcurrentHashMap<>();

    @Override
    public Optional<Guest> findByCodeHash(String codeHash) {
        return Optional.ofNullable(guestsByCodeHash.get(codeHash));
    }

    @Override
    public Optional<Stay> findStay(UUID stayId) {
        return Optional.ofNullable(stays.get(stayId));
    }

    @Override
    public void save(Stay stay) {
        stays.put(stay.id(), stay);
    }

    @Override
    public void save(Guest guest) {
        guestsByCodeHash.put(guest.codeHash(), guest);
    }
}
