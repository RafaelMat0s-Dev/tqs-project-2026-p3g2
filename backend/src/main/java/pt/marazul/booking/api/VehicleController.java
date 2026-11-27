package pt.marazul.booking.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.marazul.booking.application.VehicleService;
import pt.marazul.booking.domain.Vehicle;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COORDINATOR')")
    public VehicleResponse register(@Valid @RequestBody RegisterVehicleRequest request) {
        return VehicleResponse.from(service.register(request.hubId(), request.type(), request.batteryPercentage()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COORDINATOR')")
    public VehicleResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeStatusRequest request) {
        return VehicleResponse.from(service.changeStatus(id, request.status()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'COORDINATOR', 'TECHNICIAN', 'CLIENT_MANAGER')")
    public List<VehicleResponse> activeVehicles() {
        return service.activeVehicles().stream().map(VehicleResponse::from).toList();
    }

    public record RegisterVehicleRequest(@NotBlank String hubId, @NotBlank String type,
                                         @Min(0) @Max(100) int batteryPercentage) { }

    public record ChangeStatusRequest(@NotNull Vehicle.Status status) { }

    public record VehicleResponse(UUID id, String hubId, String type, int batteryPercentage, Vehicle.Status status) {
        static VehicleResponse from(Vehicle vehicle) {
            return new VehicleResponse(vehicle.id(), vehicle.hubId(), vehicle.type(), vehicle.batteryPercentage(), vehicle.status());
        }
    }
}
