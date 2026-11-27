package pt.marazul.identity.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.marazul.identity.application.JwtService;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffAccount;
import pt.marazul.identity.domain.StaffRole;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {
    public static final String AUTH_COOKIE = "marazul_session";

    private final StaffAccountService accounts;
    private final JwtService jwt;

    public IdentityController(StaffAccountService accounts, JwtService jwt) {
        this.accounts = accounts;
        this.jwt = jwt;
    }

    @PostMapping("/staff")
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public StaffResponse create(@Valid @RequestBody CreateStaffRequest request) {
        return StaffResponse.from(accounts.create(request.email(), request.displayName(), request.password(), request.role()));
    }

    @PostMapping("/login")
    public StaffResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        StaffAccount account = accounts.authenticate(request.email(), request.password());
        ResponseCookie cookie = ResponseCookie.from(AUTH_COOKIE, jwt.issue(account))
                .httpOnly(true).secure(true).sameSite("Strict").path("/").maxAge(Duration.ofHours(8)).build();
        response.addHeader("Set-Cookie", cookie.toString());
        return StaffResponse.from(account);
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    public record CsrfResponse(String headerName, String token) { }

    public record CreateStaffRequest(
            @NotBlank @Email String email,
            @NotBlank String displayName,
            @NotBlank @Size(min = 12) String password,
            @NotNull StaffRole role) { }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) { }

    public record StaffResponse(UUID id, String email, String displayName, StaffRole role) {
        static StaffResponse from(StaffAccount account) {
            return new StaffResponse(account.id(), account.email(), account.displayName(), account.role());
        }
    }
}
