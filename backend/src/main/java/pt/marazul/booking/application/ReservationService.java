package pt.marazul.booking.application;

import org.springframework.stereotype.Service;
import pt.marazul.booking.domain.Reservation;
import pt.marazul.booking.domain.ReservationConflict;
import pt.marazul.booking.domain.ReservationRuleViolation;
import pt.marazul.booking.domain.Vehicle;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ReservationService {
    private static final int MINIMUM_BATTERY_PERCENTAGE = 25;

    private final VehicleRepository vehicles;
    private final ReservationRepository reservations;

    public ReservationService(VehicleRepository vehicles, ReservationRepository reservations) {
        this.vehicles = vehicles;
        this.reservations = reservations;
    }

    public Reservation reserve(UUID vehicleId, String guestId, Instant startsAt, Instant endsAt) {
        if (!startsAt.isBefore(endsAt)) {
            throw new ReservationRuleViolation("BR7", "O período da reserva é inválido.");
        }
        Vehicle vehicle = vehicles.findById(vehicleId)
                .orElseThrow(() -> new ReservationRuleViolation("BR1", "Veículo inexistente."));
        if (!reservations.findOverlapping(vehicleId, startsAt, endsAt).isEmpty()) {
            throw new ReservationConflict("O veículo já está reservado nesse período.");
        }
        if (vehicle.status() != Vehicle.Status.AVAILABLE || vehicle.batteryPercentage() < MINIMUM_BATTERY_PERCENTAGE) {
            throw new ReservationRuleViolation("BR1", "O veículo não está disponível ou não tem bateria suficiente.");
        }
        if (!reservations.findActiveForGuest(guestId, startsAt, endsAt).isEmpty()) {
            throw new ReservationRuleViolation("BR8", "O hóspede já tem uma reserva ativa.");
        }
        Reservation reservation = new Reservation(UUID.randomUUID(), vehicleId, guestId, startsAt, endsAt);
        reservations.save(reservation);
        vehicles.save(vehicle.withStatus(Vehicle.Status.RESERVED));
        return reservation;
    }

    public Optional<Reservation> find(UUID reservationId) {
        return reservations.findById(reservationId);
    }

    public List<Reservation> findReservedStartedBefore(Instant instant) {
        return reservations.findReservedStartedBefore(instant);
    }

    public void markUnlocked(UUID reservationId) {
        Reservation reservation = reserved(reservationId);
        reservations.save(reservation.withStatus(Reservation.Status.CONVERTED));
        changeVehicleStatus(reservation.vehicleId(), Vehicle.Status.IN_USE);
    }

    public void expire(UUID reservationId) {
        Reservation reservation = reserved(reservationId);
        reservations.save(reservation.withStatus(Reservation.Status.EXPIRED));
        changeVehicleStatus(reservation.vehicleId(), Vehicle.Status.AVAILABLE);
    }

    private Reservation reserved(UUID reservationId) {
        return reservations.findById(reservationId)
                .filter(r -> r.status() == Reservation.Status.RESERVED)
                .orElseThrow(() -> new IllegalStateException("A reserva " + reservationId + " não está ativa."));
    }

    private void changeVehicleStatus(UUID vehicleId, Vehicle.Status status) {
        vehicles.findById(vehicleId).ifPresent(vehicle -> vehicles.save(vehicle.withStatus(status)));
    }
}
