CREATE TABLE contract_version (
    version                     INTEGER PRIMARY KEY CHECK (version >= 1),
    effective_from              TIMESTAMPTZ NOT NULL UNIQUE,
    unlock_fee_cents            INTEGER     NOT NULL CHECK (unlock_fee_cents >= 0),
    per_minute_cents            INTEGER     NOT NULL CHECK (per_minute_cents >= 0),
    deposit_cents               INTEGER     NOT NULL CHECK (deposit_cents >= 0),
    surcharge_per_minute_cents  INTEGER     NOT NULL CHECK (surcharge_per_minute_cents >= 0),
    surcharge_tolerance_minutes INTEGER     NOT NULL CHECK (surcharge_tolerance_minutes >= 0),
    surcharge_cap_cents         INTEGER     NOT NULL CHECK (surcharge_cap_cents >= 0),
    sla_critical_hours          INTEGER     NOT NULL CHECK (sla_critical_hours >= 1),
    sla_high_hours              INTEGER     NOT NULL CHECK (sla_high_hours >= 1),
    sla_normal_hours            INTEGER     NOT NULL CHECK (sla_normal_hours >= 1),
    minimum_battery_percentage  SMALLINT    NOT NULL CHECK (minimum_battery_percentage BETWEEN 0 AND 100),
    pickup_window_minutes       INTEGER     NOT NULL CHECK (pickup_window_minutes >= 1)
);

CREATE FUNCTION contract_version_is_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'As versões do contrato são imutáveis.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER contract_version_immutable
    BEFORE UPDATE OR DELETE ON contract_version
    FOR EACH ROW EXECUTE FUNCTION contract_version_is_immutable();

INSERT INTO contract_version
    (version, effective_from, unlock_fee_cents, per_minute_cents, deposit_cents,
     surcharge_per_minute_cents, surcharge_tolerance_minutes, surcharge_cap_cents,
     sla_critical_hours, sla_high_hours, sla_normal_hours,
     minimum_battery_percentage, pickup_window_minutes)
VALUES
    (1, TIMESTAMPTZ '1970-01-01 00:00:00+00', 100, 5, 2500, 10, 5, 2500, 4, 24, 72, 25, 15);
