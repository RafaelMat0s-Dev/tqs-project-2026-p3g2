package pt.marazul.cucumber.steps;

import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import pt.marazul.booking.api.ReservationFacade;
import pt.marazul.booking.api.ReservationView;
import pt.marazul.booking.application.ReservationService;
import pt.marazul.booking.application.VehicleRepository;
import pt.marazul.booking.domain.Vehicle;
import pt.marazul.cucumber.MutableClock;
import pt.marazul.shared.infrastructure.ClockConfiguration;
import pt.marazul.trip.application.ReservationExpiryService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TripSteps {
    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);
    private static final Map<String, Vehicle.Status> VEHICLE_STATES =
            Map.of("Disponível", Vehicle.Status.AVAILABLE, "Reservada", Vehicle.Status.RESERVED, "Em uso", Vehicle.Status.IN_USE);
    private static final Map<String, ReservationView.State> RESERVATION_STATES =
            Map.of("Reservada", ReservationView.State.RESERVED, "Expirada", ReservationView.State.EXPIRED);

    private final ApiSteps api;
    private final MutableClock clock;
    private final ReservationService reservations;
    private final ReservationFacade facade;
    private final VehicleRepository vehicles;
    private final ReservationExpiryService expiry;
    private final Map<String, String> guests = new HashMap<>();
    private UUID vehicleId;
    private UUID reservationId;
    private Instant start;

    public TripSteps(ApiSteps api, MutableClock clock, ReservationService reservations, ReservationFacade facade,
                     VehicleRepository vehicles, ReservationExpiryService expiry) {
        this.api = api;
        this.clock = clock;
        this.reservations = reservations;
        this.facade = facade;
        this.vehicles = vehicles;
        this.expiry = expiry;
    }

    @Dado("uma reserva da/do {word} que começou às {int}h{int}")
    public void umaReservaQueComecouAs(String guest, int hour, int minute) {
        vehicleId = UUID.randomUUID();
        vehicles.save(new Vehicle(vehicleId, "hotel-alvina-mar", "E_BIKE", 80, Vehicle.Status.AVAILABLE));
        start = at(hour, minute, 0);
        clock.set(start);
        reservationId = reservations.reserve(vehicleId, guest(guest), start, start.plusSeconds(3600)).id();
    }

    @Quando("a/o {word} desbloqueia às {int}h{int} e {int} s")
    public void desbloqueiaAs(String guest, int hour, int minute, int second) {
        clock.set(at(hour, minute, second));
        api.envioOPedido("/api/trips",
                "{\"reservationId\":\"" + reservationId + "\",\"guestId\":\"" + guest(guest) + "\"}");
    }

    @Quando("passam {int} minutos e {int} s sobre o início")
    public void passamMinutosSobreOInicio(int minutes, int seconds) {
        clock.set(start.plusSeconds(minutes * 60L + seconds));
        expiry.expireOverdue();
    }

    @Então("a viagem começa")
    public void aViagemComeca() {
        api.aRespostaTemOEstado(201);
        assertThat(api.lastResponse().jsonPath().getString("reservationId")).isEqualTo(reservationId.toString());
    }

    @Então("a bicicleta passa a {string}")
    public void aBicicletaPassaA(String state) {
        assertThat(vehicles.findById(vehicleId)).map(Vehicle::status).hasValue(VEHICLE_STATES.get(state));
    }

    @Então("a reserva passa a {string}")
    public void aReservaPassaA(String state) {
        assertThat(facade.find(reservationId)).map(ReservationView::state).hasValue(RESERVATION_STATES.get(state));
    }

    private String guest(String name) {
        return guests.computeIfAbsent(name, n -> n.toLowerCase() + "-" + UUID.randomUUID());
    }

    private static Instant at(int hour, int minute, int second) {
        return DAY.atTime(LocalTime.of(hour, minute, second)).atZone(ClockConfiguration.OPERATING_ZONE).toInstant();
    }
}
