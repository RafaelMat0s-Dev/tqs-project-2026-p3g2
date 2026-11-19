package pt.marazul.domain;

import java.time.Instant;
import java.util.UUID;

public record Reservation(UUID id, UUID vehicleId, String guestId, Instant startsAt, Instant endsAt) { }
