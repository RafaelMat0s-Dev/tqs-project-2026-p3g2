package pt.marazul.fleet.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import pt.marazul.fleet.application.VehicleNotFound;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasAnyRole('COORDINATOR','TECHNICIAN')")
public class VehicleController {
    private final FleetApi fleet;
    private final JsonMapper json;
    private final Validator validator;

    public VehicleController(FleetApi fleet, JsonMapper json, Validator validator) {
        this.fleet = fleet;
        this.json = json;
        this.validator = validator;
    }

    @PostMapping
    @Operation(summary = "Registar veículo (nasce desativado)")
    @ApiResponse(responseCode = "201", description = "Veículo registado")
    @ApiResponse(responseCode = "409", description = "Código já existente (VEHICLE_CODE)")
    public ResponseEntity<VehicleView> register(@RequestBody String body) {
        RegisterVehicleRequest request = parse(body);
        VehicleView view = fleet.register(request.code(), request.type(), request.baseHubCode());
        return ResponseEntity.created(URI.create("/api/vehicles/" + view.id())).body(view);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativar veículo")
    @ApiResponse(responseCode = "422", description = "Transição inválida (VEHICLE_LIFECYCLE)")
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    public VehicleView activate(@PathVariable UUID id) {
        return fleet.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativar veículo")
    @ApiResponse(responseCode = "422", description = "Transição inválida (VEHICLE_LIFECYCLE)")
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    public VehicleView deactivate(@PathVariable UUID id) {
        return fleet.deactivate(id);
    }

    @PatchMapping("/{id}/retire")
    @Operation(summary = "Abater veículo")
    @ApiResponse(responseCode = "422", description = "Transição inválida (VEHICLE_LIFECYCLE)")
    @ApiResponse(responseCode = "404", description = "Veículo não encontrado")
    public VehicleView retire(@PathVariable UUID id) {
        return fleet.retire(id);
    }

    @GetMapping
    @Operation(summary = "Listar veículos, filtrando por estado e hub")
    public List<VehicleView> list(@RequestParam Optional<VehicleView.Status> status,
            @RequestParam Optional<String> hub) {
        return fleet.list(status, hub);
    }

    // O corpo é lido aqui, depois do @PreAuthorize, para que um papel sem permissão receba 403 e não 400.
    private RegisterVehicleRequest parse(String body) {
        try {
            RegisterVehicleRequest request = json.readValue(body, RegisterVehicleRequest.class);
            if (request != null && validator.validate(request).isEmpty()) {
                return request;
            }
        } catch (JacksonException ignored) {
            // cai no 400 abaixo
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido de registo de veículo inválido");
    }

    @ExceptionHandler(VehicleNotFound.class)
    ProblemDetail notFound(VehicleNotFound exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Veículo não encontrado");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    public record RegisterVehicleRequest(@NotNull @Pattern(regexp = "B-\\d{2,3}") String code,
            @NotBlank @Size(max = 32) String type, @NotBlank @Size(max = 64) String baseHubCode) { }
}
