package pt.marazul.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.marazul.identity.application.GuestLoginService;
import pt.marazul.identity.application.GuestSession;
import pt.marazul.identity.application.InvalidGuestCode;
import pt.marazul.identity.application.LoginResult;
import pt.marazul.identity.application.TooManyLoginAttempts;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/guest")
public class GuestAuthController {
    private static final String BEARER = "Bearer ";

    private final GuestLoginService login;

    public GuestAuthController(GuestLoginService login) {
        this.login = login;
    }

    @PostMapping
    public GuestLoginResponse login(@Valid @RequestBody GuestLoginRequest body, HttpServletRequest request) {
        LoginResult result = login.login(body.code(), request.getRemoteAddr());
        GuestSession session = result.session();
        return new GuestLoginResponse(result.token(), session.guestId(), session.name(), session.expiresAt());
    }

    @GetMapping("/session")
    public ResponseEntity<?> session(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                     HttpServletRequest request) {
        if (authorization != null && authorization.startsWith(BEARER)) {
            var session = login.session(authorization.substring(BEARER.length()).strip());
            if (session.isPresent()) {
                GuestSession found = session.get();
                return ResponseEntity.ok(new GuestSessionResponse(found.guestId(), found.name(), found.expiresAt()));
            }
        }
        ProblemDetail problem = problem(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada",
                "A sessão não é válida ou já expirou.", request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(InvalidGuestCode.class)
    ProblemDetail invalidCode(InvalidGuestCode exception, HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "Código inválido", exception.getMessage(), request);
    }

    @ExceptionHandler(TooManyLoginAttempts.class)
    ResponseEntity<ProblemDetail> tooManyAttempts(TooManyLoginAttempts exception, HttpServletRequest request) {
        Duration wait = exception.retryAfter();
        long seconds = wait.toSeconds() + (wait.toNanosPart() > 0 ? 1 : 0);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(seconds))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas tentativas", exception.getMessage(), request));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    public record GuestLoginRequest(@NotBlank String code) { }

    public record GuestLoginResponse(String token, UUID guestId, String name, Instant expiresAt) { }

    public record GuestSessionResponse(UUID guestId, String name, Instant expiresAt) { }
}
