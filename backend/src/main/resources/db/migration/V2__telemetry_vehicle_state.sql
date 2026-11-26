CREATE TABLE telemetry_vehicle_state (
    vehicle_code      VARCHAR(64) PRIMARY KEY,
    battery_percentage SMALLINT    NOT NULL CHECK (battery_percentage BETWEEN 0 AND 100),
    locked            BOOLEAN     NOT NULL,
    hub_id            VARCHAR(64),
    shock             BOOLEAN     NOT NULL,
    last_reading_at   TIMESTAMPTZ NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL
);
