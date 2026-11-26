package pt.marazul.shared.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pt.marazul.shared.domain.BusinessRuleConflict;
import pt.marazul.shared.domain.BusinessRuleViolation;

import java.net.URI;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    static final URI RULE_TYPE = URI.create("urn:marazul:erro:regra-de-negocio");
    static final URI CONFLICT_TYPE = URI.create("urn:marazul:erro:conflito");

    @ExceptionHandler(BusinessRuleConflict.class)
    ProblemDetail handleConflict(BusinessRuleConflict exception, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, CONFLICT_TYPE, "Conflito com outro pedido", exception, request);
    }

    @ExceptionHandler(BusinessRuleViolation.class)
    ProblemDetail handleRuleViolation(BusinessRuleViolation exception, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, RULE_TYPE, "Regra de negócio violada", exception, request);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, String title,
                                         BusinessRuleViolation exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(type);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("rule", exception.rule());
        return problem;
    }
}
