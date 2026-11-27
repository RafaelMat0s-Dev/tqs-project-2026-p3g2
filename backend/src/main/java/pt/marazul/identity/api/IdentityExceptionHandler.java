package pt.marazul.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.marazul.identity.application.StaffAccountService;

import java.net.URI;

@RestControllerAdvice
public class IdentityExceptionHandler {
    @ExceptionHandler(StaffAccountService.DuplicateStaffEmailException.class)
    ProblemDetail duplicateEmail(StaffAccountService.DuplicateStaffEmailException exception,
                                 HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "urn:marazul:erro:conflito", "Conflito com outro pedido",
                exception, request);
    }

    @ExceptionHandler(StaffAccountService.InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(StaffAccountService.InvalidCredentialsException exception,
                                     HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "urn:marazul:erro:credenciais-invalidas", "Credenciais inválidas",
                exception, request);
    }

    @ExceptionHandler(StaffAccountService.InvalidStaffAccountException.class)
    ProblemDetail invalidAccount(StaffAccountService.InvalidStaffAccountException exception,
                                 HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "urn:marazul:erro:conta-invalida", "Conta inválida",
                exception, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, RuntimeException exception,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(URI.create(type));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
