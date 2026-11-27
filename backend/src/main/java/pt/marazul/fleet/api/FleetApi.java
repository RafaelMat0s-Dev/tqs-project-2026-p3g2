package pt.marazul.fleet.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.marazul.fleet.application.FleetService;
import pt.marazul.fleet.domain.VehicleStatus;

@Component
public class FleetApi {
    private final FleetService service;

    public FleetApi(FleetService service) {
        this.service = service;
    }

    public VehicleView register(String code, String type, String baseHubCode) {
        return VehicleView.of(service.register(code, type, baseHubCode));
    }

    public VehicleView activate(UUID id) {
        return VehicleView.of(service.activate(id));
    }

    public VehicleView deactivate(UUID id) {
        return VehicleView.of(service.deactivate(id));
    }

    public VehicleView retire(UUID id) {
        return VehicleView.of(service.retire(id));
    }

    public VehicleView markReserved(UUID id) {
        return VehicleView.of(service.markReserved(id));
    }

    public VehicleView release(UUID id) {
        return VehicleView.of(service.release(id));
    }

    public VehicleView startTrip(UUID id) {
        return VehicleView.of(service.startTrip(id));
    }

    public VehicleView endTrip(UUID id) {
        return VehicleView.of(service.endTrip(id));
    }

    public VehicleView sendToMaintenance(UUID id) {
        return VehicleView.of(service.sendToMaintenance(id));
    }

    public VehicleView completeMaintenance(UUID id) {
        return VehicleView.of(service.completeMaintenance(id));
    }

    public Optional<VehicleView> findById(UUID id) {
        return service.findById(id).map(VehicleView::of);
    }

    public Optional<VehicleView> findByCode(String code) {
        return service.findByCode(code).map(VehicleView::of);
    }

    public List<VehicleView> list(Optional<VehicleView.Status> status, Optional<String> hub) {
        return service.list(status.map(s -> VehicleStatus.valueOf(s.name())), hub).stream()
                .map(VehicleView::of).toList();
    }
}
