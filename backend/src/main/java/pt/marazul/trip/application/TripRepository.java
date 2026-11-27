package pt.marazul.trip.application;

import pt.marazul.trip.domain.Trip;

public interface TripRepository {
    Trip save(Trip trip);
}
