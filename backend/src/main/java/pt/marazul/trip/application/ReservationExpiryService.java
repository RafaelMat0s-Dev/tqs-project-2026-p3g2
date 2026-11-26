package pt.marazul.trip.application;

import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ReservationExpiryService {
    private final ReservationPort reservations;
    private final PickupWindowPort contract;
    private final Clock clock;

    public ReservationExpiryService(ReservationPort reservations, PickupWindowPort contract, Clock clock) {
        this.reservations = reservations;
        this.contract = contract;
        this.clock = clock;
    }

    public int expireOverdue() {
        var overdue = reservations.findReservedStartedBefore(clock.instant().minus(contract.pickupWindow()));
        overdue.forEach(reservation -> reservations.expire(reservation.id()));
        return overdue.size();
    }
}
