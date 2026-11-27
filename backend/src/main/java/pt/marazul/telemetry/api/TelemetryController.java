package pt.marazul.telemetry.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pt.marazul.telemetry.application.TelemetryIngestionService;

import java.time.Instant;

@RestController
@RequestMapping("/api/telemetry/vehicles")
public class TelemetryController {
    private final TelemetryIngestionService service;

    public TelemetryController(TelemetryIngestionService service) {
        this.service = service;
    }

    @GetMapping("/{code}")
    public VehicleStateResponse state(@PathVariable String code) {
        return service.latest(code)
                .map(reading -> new VehicleStateResponse(reading.vehicleCode(), reading.batteryPercentage(),
                        reading.locked(), reading.hubId(), reading.shock(), reading.originAt()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Não há telemetria para o veículo " + code + "."));
    }

    public record VehicleStateResponse(String vehicleCode, int batteryPercentage, boolean locked, String hubId,
                                       boolean shock, Instant lastReadingAt) { }
}
