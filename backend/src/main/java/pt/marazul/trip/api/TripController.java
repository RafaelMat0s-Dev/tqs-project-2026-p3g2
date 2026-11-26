package pt.marazul.trip.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.marazul.trip.application.NotTheGuestsReservation;
import pt.marazul.trip.application.ReservationNotFound;
import pt.marazul.trip.application.UnlockService;
import pt.marazul.trip.domain.Trip;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/trips")
public class TripController {
    private final UnlockService unlock;

    public TripController(UnlockService unlock) {
        this.unlock = unlock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse unlock(@Valid @RequestBody UnlockRequest request) {
        return TripResponse.from(unlock.unlock(request.reservationId(), request.guestId()));
    }

    @ExceptionHandler(NotTheGuestsReservation.class)
    ProblemDetail forbidden(NotTheGuestsReservation exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Sem permissão", exception, request);
    }

    @ExceptionHandler(ReservationNotFound.class)
    ProblemDetail notFound(ReservationNotFound exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Reserva não encontrada", exception, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, RuntimeException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    public record UnlockRequest(@NotNull UUID reservationId, @NotBlank String guestId) { }

    public record TripResponse(UUID id, UUID reservationId, UUID vehicleId, Instant startedAt) {
        static TripResponse from(Trip trip) {
            return new TripResponse(trip.id(), trip.reservationId(), trip.vehicleId(), trip.startedAt());
        }
    }
}
