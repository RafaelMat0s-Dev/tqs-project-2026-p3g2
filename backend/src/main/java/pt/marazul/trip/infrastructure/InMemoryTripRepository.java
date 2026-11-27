package pt.marazul.trip.infrastructure;

import org.springframework.stereotype.Repository;
import pt.marazul.trip.application.TripRepository;
import pt.marazul.trip.domain.Trip;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryTripRepository implements TripRepository {
    private final Map<UUID, Trip> trips = new ConcurrentHashMap<>();

    @Override
    public Trip save(Trip trip) {
        trips.put(trip.id(), trip);
        return trip;
    }
}
