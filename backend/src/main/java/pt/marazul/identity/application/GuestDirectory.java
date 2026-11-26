package pt.marazul.identity.application;

import pt.marazul.identity.domain.Guest;
import pt.marazul.identity.domain.Stay;

import java.util.Optional;
import java.util.UUID;

public interface GuestDirectory {
    Optional<Guest> findByCodeHash(String codeHash);

    Optional<Stay> findStay(UUID stayId);

    void save(Stay stay);

    void save(Guest guest);
}
