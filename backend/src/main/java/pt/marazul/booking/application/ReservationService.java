package pt.marazul.booking.application;

import org.springframework.stereotype.Service;
import pt.marazul.booking.domain.Reservation;
import pt.marazul.booking.domain.ReservationConflict;
import pt.marazul.booking.domain.ReservationRuleViolation;
import pt.marazul.booking.domain.Vehicle;

import java.time.Instant;
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
        vehicles.save(new Vehicle(vehicle.id(), vehicle.hubId(), vehicle.type(), vehicle.batteryPercentage(), Vehicle.Status.RESERVED));
        return reservation;
    }
}
