package pt.marazul.identity.application;

import java.time.Instant;
import java.util.UUID;

public record GuestSession(UUID guestId, String name, Instant expiresAt) { }
