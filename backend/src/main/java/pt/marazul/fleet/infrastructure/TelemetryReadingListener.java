package pt.marazul.fleet.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pt.marazul.fleet.application.FleetService;
import pt.marazul.telemetry.api.TelemetryReadingReceived;

@Component
public class TelemetryReadingListener {
    private static final Logger log = LoggerFactory.getLogger(TelemetryReadingListener.class);

    private final FleetService fleet;

    public TelemetryReadingListener(FleetService fleet) {
        this.fleet = fleet;
    }

    @EventListener
    public void on(TelemetryReadingReceived event) {
        try {
            fleet.applyReading(event.vehicleCode(), event.batteryPercentage(), event.originAt());
        } catch (RuntimeException e) {
            log.warn("Falha ao aplicar a leitura de {}", event.vehicleCode(), e);
        }
    }
}
