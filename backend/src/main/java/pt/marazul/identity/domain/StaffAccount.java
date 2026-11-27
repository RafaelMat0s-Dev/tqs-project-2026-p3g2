package pt.marazul.identity.domain;

import java.util.UUID;

public record StaffAccount(
        UUID id,
        String email,
        String displayName,
        String passwordHash,
        StaffRole role,
        boolean active) {

    public StaffAccount deactivate() {
        return new StaffAccount(id, email, displayName, passwordHash, role, false);
    }
}
