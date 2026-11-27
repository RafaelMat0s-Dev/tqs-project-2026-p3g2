package pt.marazul.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.marazul.fleet.application.FleetService;
import pt.marazul.fleet.application.InMemoryVehicles;
import pt.marazul.fleet.domain.VehicleStatus;

class FleetApiTest {
    private static final Instant T = Instant.parse("2026-11-20T14:31:07Z");

    private FleetService service;
    private FleetApi api;

    @BeforeEach
    void setUp() {
        service = new FleetService(new InMemoryVehicles());
        api = new FleetApi(service);
    }

    @Test
    void viewStatusHasTheSameSixNamesAsTheDomainStatus() {
        assertThat(Arrays.stream(VehicleView.Status.values()).map(Enum::name))
                .containsExactly(Arrays.stream(VehicleStatus.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void registerReturnsViewOfDisabledVehicleWithUnknownBattery() {
        VehicleView v = api.register("B-17", "BICYCLE", "sao-bras");

        assertThat(v.code()).isEqualTo("B-17");
        assertThat(v.type()).isEqualTo("BICYCLE");
        assertThat(v.baseHubCode()).isEqualTo("sao-bras");
        assertThat(v.status()).isEqualTo(VehicleView.Status.DISABLED);
        assertThat(v.batteryPercentage()).isNull();
        assertThat(v.lastReadingAt()).isNull();
    }

    @Test
    void findByIdAndByCodeConvertTheVehicle() {
        VehicleView registered = api.register("B-17", "BICYCLE", "sao-bras");
        service.applyReading("B-17", 47, T);

        assertThat(api.findById(registered.id())).hasValueSatisfying(v -> {
            assertThat(v.batteryPercentage()).isEqualTo(47);
            assertThat(v.lastReadingAt()).isEqualTo(T);
        });
        assertThat(api.findByCode("B-17")).map(VehicleView::id).contains(registered.id());
        assertThat(api.findByCode("B-99")).isEmpty();
    }

    @Test
    void transitionsDelegateAndReturnTheNewView() {
        var id = api.register("B-17", "BICYCLE", "sao-bras").id();

        assertThat(api.activate(id).status()).isEqualTo(VehicleView.Status.AVAILABLE);
        assertThat(api.markReserved(id).status()).isEqualTo(VehicleView.Status.RESERVED);
        assertThat(api.release(id).status()).isEqualTo(VehicleView.Status.AVAILABLE);
        assertThat(api.markReserved(id).status()).isEqualTo(VehicleView.Status.RESERVED);
        assertThat(api.startTrip(id).status()).isEqualTo(VehicleView.Status.IN_USE);
        assertThat(api.endTrip(id).status()).isEqualTo(VehicleView.Status.AVAILABLE);
        assertThat(api.deactivate(id).status()).isEqualTo(VehicleView.Status.DISABLED);
        assertThat(api.sendToMaintenance(id).status()).isEqualTo(VehicleView.Status.MAINTENANCE);
        assertThat(api.completeMaintenance(id).status()).isEqualTo(VehicleView.Status.AVAILABLE);
        assertThat(api.sendToMaintenance(id).status()).isEqualTo(VehicleView.Status.MAINTENANCE);
        assertThat(api.retire(id).status()).isEqualTo(VehicleView.Status.RETIRED);
        assertThat(service.findById(id).orElseThrow().status()).isEqualTo(VehicleStatus.RETIRED);
    }

    @Test
    void listConvertsStatusFilterAndFiltersByHub() {
        var b17 = api.register("B-17", "BICYCLE", "sao-bras");
        api.register("B-18", "BICYCLE", "praia-da-ribeira");
        api.activate(b17.id());

        assertThat(api.list(Optional.of(VehicleView.Status.AVAILABLE), Optional.empty()))
                .extracting(VehicleView::code).containsExactly("B-17");
        assertThat(api.list(Optional.empty(), Optional.of("praia-da-ribeira")))
                .extracting(VehicleView::code).containsExactly("B-18");
        assertThat(api.list(Optional.empty(), Optional.empty())).hasSize(2);
    }
}
