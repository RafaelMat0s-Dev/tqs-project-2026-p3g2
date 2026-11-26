package pt.marazul.trip.domain;

import java.time.Instant;
import java.util.UUID;

public record Trip(UUID id, UUID reservationId, UUID vehicleId, String guestId, Instant startedAt) { }
