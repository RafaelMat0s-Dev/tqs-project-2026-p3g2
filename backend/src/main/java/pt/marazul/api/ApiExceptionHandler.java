package pt.marazul.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.marazul.domain.ReservationConflict;
import pt.marazul.domain.ReservationRuleViolation;
import pt.marazul.domain.FleetRuleViolation;
import pt.marazul.identity.application.StaffAccountService;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ReservationConflict.class)
    ProblemDetail conflict(ReservationConflict exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflito com outro pedido", exception, request);
    }

    @ExceptionHandler(ReservationRuleViolation.class)
    ProblemDetail ruleViolation(ReservationRuleViolation exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", exception, request);
    }

    @ExceptionHandler(FleetRuleViolation.class)
    ProblemDetail fleetRuleViolation(FleetRuleViolation exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setType(URI.create("urn:marazul:erro:regra-de-negocio"));
        problem.setTitle("Regra de negócio violada");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("rule", exception.rule());
        return problem;
    }

    @ExceptionHandler(StaffAccountService.DuplicateStaffEmailException.class)
    ProblemDetail duplicateEmail(StaffAccountService.DuplicateStaffEmailException exception,
                                  HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setType(URI.create("urn:marazul:erro:conflito"));
        problem.setTitle("Conflito com outro pedido");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(StaffAccountService.InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(StaffAccountService.InvalidCredentialsException exception,
                                      HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setType(URI.create("urn:marazul:erro:credenciais-invalidas"));
        problem.setTitle("Credenciais inválidas");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static ProblemDetail problem(HttpStatus status, String title, ReservationRuleViolation exception,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(URI.create("urn:marazul:erro:regra-de-negocio"));
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("rule", exception.rule());
        return problem;
    }
}
