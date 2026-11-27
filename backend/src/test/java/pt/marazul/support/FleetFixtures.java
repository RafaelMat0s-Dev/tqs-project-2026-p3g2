package pt.marazul.support;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.ApplicationEventPublisher;
import pt.marazul.fleet.api.FleetApi;
import pt.marazul.fleet.api.VehicleView;
import pt.marazul.telemetry.api.TelemetryReadingReceived;

/** Creates vehicles through the fleet, as the booking now sees them. */
public final class FleetFixtures {
    private static final String HUB = "hotel-alvina-mar";
    private static final Instant READING_AT = Instant.parse("2026-11-20T14:31:07Z");

    private FleetFixtures() { }

    /** Registered and activated, with no telemetry reading (battery unknown). */
    public static VehicleView activeVehicle(FleetApi fleet) {
        for (int attempt = 0; attempt < 50; attempt++) {
            String code = "B-" + ThreadLocalRandom.current().nextInt(100, 1000);
            if (fleet.findByCode(code).isPresent()) {
                continue;
            }
            return fleet.activate(fleet.register(code, "E_BIKE", HUB).id());
        }
        throw new IllegalStateException("sem código B-xx livre");
    }

    /** AVAILABLE in the fleet with the given battery; returns its id. */
    public static UUID availableVehicle(FleetApi fleet, ApplicationEventPublisher events, int battery) {
        VehicleView vehicle = activeVehicle(fleet);
        events.publishEvent(new TelemetryReadingReceived(vehicle.code(), READING_AT, battery, true, HUB, false));
        return vehicle.id();
    }
}
