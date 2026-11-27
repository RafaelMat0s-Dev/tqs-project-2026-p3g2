package pt.marazul.cucumber.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import pt.marazul.contract.api.ContractFacade;
import pt.marazul.contract.api.ContractView;
import pt.marazul.identity.application.StaffAccountService;
import pt.marazul.identity.domain.StaffRole;

import java.util.Map;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class ContractSteps {
    private static final String PASSWORD = "uma-palavra-passe-segura";
    private static final Map<String, StaffRole> ROLES = Map.of(
            "administradora", StaffRole.ADMINISTRATOR,
            "operador", StaffRole.COORDINATOR,
            "gestora do cliente", StaffRole.CLIENT_MANAGER);
    private static final IntFunction<String> EUROS = cents -> String.format("%d,%02d €", cents / 100, cents % 100);
    private static final IntFunction<String> MINUTES = minutes -> minutes + " min";
    private static final IntFunction<String> HOURS = hours -> hours + " h";

    private final ApiSteps api;
    private final StaffAccountService accounts;
    private final ContractFacade contract;
    private int tripMinutes;
    private int tripVersion;
    private int chargeWhenBilled;

    public ContractSteps(ApiSteps api, StaffAccountService accounts, ContractFacade contract) {
        this.api = api;
        this.accounts = accounts;
        this.contract = contract;
    }

    @Dado("a/o {word} autenticada/autenticado como {string}")
    public void autenticadaComo(String name, String role) {
        String email = name.toLowerCase() + "-" + System.nanoTime() + "@marazul.test";
        accounts.create(email, name, PASSWORD, ROLES.get(role));
        api.autenticar(email, PASSWORD);
    }

    @Dado("uma viagem de {int} minutos cobrada com a versão {int} do contrato")
    public void umaViagemCobradaComAVersao(int minutes, int version) {
        tripMinutes = minutes;
        tripVersion = version;
        chargeWhenBilled = chargeWith(version);
    }

    @Quando("a/o {word} publica a versão {int} do contrato com {string} por minuto a partir de {string}")
    public void publicaAVersao(String name, int version, String perMinute, String effectiveFrom) {
        ContractView base = contract.version(1).orElseThrow();
        String body = "{\"effectiveFrom\":\"" + effectiveFrom + "\",\"unlockFeeCents\":" + base.unlockFeeCents()
                + ",\"perMinuteCents\":" + cents(perMinute) + ",\"depositCents\":" + base.depositCents()
                + ",\"surchargePerMinuteCents\":" + base.surchargePerMinuteCents()
                + ",\"surchargeToleranceMinutes\":" + base.surchargeToleranceMinutes()
                + ",\"surchargeCapCents\":" + base.surchargeCapCents()
                + ",\"slaCriticalHours\":" + base.slaCriticalHours() + ",\"slaHighHours\":" + base.slaHighHours()
                + ",\"slaNormalHours\":" + base.slaNormalHours()
                + ",\"minimumBatteryPercentage\":" + base.minimumBatteryPercentage()
                + ",\"pickupWindowMinutes\":" + base.pickupWindowMinutes() + "}";
        api.envioOPedido("/api/contract/versions", body);
        if (api.lastResponse().statusCode() == 201) {
            assertThat(api.lastResponse().jsonPath().getInt("version")).isEqualTo(version);
        }
    }

    @Então("o contrato apresentado tem:")
    public void oContratoApresentadoTem(DataTable table) {
        var json = api.lastResponse().jsonPath();
        Map<String, String> actual = Map.of(
                "desbloqueio", EUROS.apply(json.getInt("unlockFeeCents")),
                "preço por minuto", EUROS.apply(json.getInt("perMinuteCents")),
                "caução", EUROS.apply(json.getInt("depositCents")),
                "sobretaxa por minuto", EUROS.apply(json.getInt("surchargePerMinuteCents")),
                "tolerância da sobretaxa", MINUTES.apply(json.getInt("surchargeToleranceMinutes")),
                "limite da sobretaxa", EUROS.apply(json.getInt("surchargeCapCents")),
                "prazo crítico", HOURS.apply(json.getInt("slaCriticalHours")),
                "prazo alto", HOURS.apply(json.getInt("slaHighHours")),
                "prazo normal", HOURS.apply(json.getInt("slaNormalHours")));
        assertThat(actual).containsAllEntriesOf(table.asMap(String.class, String.class));
    }

    @Então("a versão {int} do contrato tem {string} por minuto")
    public void aVersaoTemPorMinuto(int version, String perMinute) {
        api.consulto("/api/contract/versions/" + version);
        api.aRespostaTemOEstado(200);
        assertThat(api.lastResponse().jsonPath().getInt("perMinuteCents")).isEqualTo(cents(perMinute));
    }

    @Então("o encargo dessa viagem continua a ser {string}")
    public void oEncargoContinuaASer(String expected) {
        assertThat(chargeWith(tripVersion)).isEqualTo(chargeWhenBilled);
        assertThat(EUROS.apply(chargeWith(tripVersion))).isEqualTo(expected);
    }

    private int chargeWith(int version) {
        ContractView view = contract.version(version).orElseThrow();
        return view.unlockFeeCents() + tripMinutes * view.perMinuteCents();
    }

    private static int cents(String euros) {
        return Integer.parseInt(euros.replace(" €", "").replace(",", ""));
    }
}
