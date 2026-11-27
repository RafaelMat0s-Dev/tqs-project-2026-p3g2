package pt.marazul.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.marazul.application.ReservationService;
import pt.marazul.domain.Reservation;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class ReservationController {
    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse reserve(@Valid @RequestBody ReservationRequest request) {
        Reservation reservation = service.reserve(request.vehicleId(), request.guestId(), request.startsAt(), request.endsAt());
        return ReservationResponse.from(reservation);
    }

    public record ReservationRequest(
            @NotNull UUID vehicleId,
            @NotBlank String guestId,
            @NotNull @Future Instant startsAt,
            @NotNull @Future Instant endsAt) { }

    public record ReservationResponse(UUID id, UUID vehicleId, String guestId, Instant startsAt, Instant endsAt) {
        static ReservationResponse from(Reservation reservation) {
            return new ReservationResponse(reservation.id(), reservation.vehicleId(), reservation.guestId(), reservation.startsAt(), reservation.endsAt());
        }
    }

}
