package pt.marazul.contract.infrastructure;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import pt.marazul.contract.application.ContractVersionRepository;
import pt.marazul.contract.domain.ContractTerms;
import pt.marazul.contract.domain.ContractVersion;
import pt.marazul.shared.domain.BusinessRuleConflict;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
class JdbcContractVersionRepository implements ContractVersionRepository {
    private static final String FIND_ALL = """
            SELECT version, effective_from, unlock_fee_cents, per_minute_cents, deposit_cents,
                   surcharge_per_minute_cents, surcharge_tolerance_minutes, surcharge_cap_cents,
                   sla_critical_hours, sla_high_hours, sla_normal_hours,
                   minimum_battery_percentage, pickup_window_minutes
            FROM contract_version ORDER BY version
            """;
    private static final String INSERT = """
            INSERT INTO contract_version
                (version, effective_from, unlock_fee_cents, per_minute_cents, deposit_cents,
                 surcharge_per_minute_cents, surcharge_tolerance_minutes, surcharge_cap_cents,
                 sla_critical_hours, sla_high_hours, sla_normal_hours,
                 minimum_battery_percentage, pickup_window_minutes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    JdbcContractVersionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ContractVersion> findAll() {
        return jdbc.query(FIND_ALL, (rs, row) -> new ContractVersion(
                rs.getInt("version"),
                rs.getObject("effective_from", OffsetDateTime.class).toInstant(),
                new ContractTerms(
                        rs.getInt("unlock_fee_cents"),
                        rs.getInt("per_minute_cents"),
                        rs.getInt("deposit_cents"),
                        rs.getInt("surcharge_per_minute_cents"),
                        rs.getInt("surcharge_tolerance_minutes"),
                        rs.getInt("surcharge_cap_cents"),
                        rs.getInt("sla_critical_hours"),
                        rs.getInt("sla_high_hours"),
                        rs.getInt("sla_normal_hours"),
                        rs.getInt("minimum_battery_percentage"),
                        rs.getInt("pickup_window_minutes"))));
    }

    @Override
    public void save(ContractVersion version) {
        ContractTerms terms = version.terms();
        try {
            jdbc.update(INSERT, version.number(), OffsetDateTime.ofInstant(version.effectiveFrom(), ZoneOffset.UTC),
                    terms.unlockFeeCents(), terms.perMinuteCents(), terms.depositCents(),
                    terms.surchargePerMinuteCents(), terms.surchargeToleranceMinutes(), terms.surchargeCapCents(),
                    terms.slaCriticalHours(), terms.slaHighHours(), terms.slaNormalHours(),
                    terms.minimumBatteryPercentage(), terms.pickupWindowMinutes());
        } catch (DuplicateKeyException exception) {
            throw new BusinessRuleConflict("BR10", "Outra versão do contrato foi publicada entretanto; volte a tentar.");
        }
    }
}
