CREATE TABLE fleet_vehicle (
    id                 UUID PRIMARY KEY,
    code               VARCHAR(64) NOT NULL UNIQUE CHECK (code ~ '^B-[0-9]{2,3}$'),
    type               VARCHAR(32) NOT NULL,
    base_hub_code      VARCHAR(64) NOT NULL,
    status             VARCHAR(16) NOT NULL CHECK (status IN ('DISABLED', 'AVAILABLE', 'RESERVED', 'IN_USE', 'MAINTENANCE', 'RETIRED')),
    battery_percentage SMALLINT CHECK (battery_percentage BETWEEN 0 AND 100),
    last_reading_at    TIMESTAMPTZ,
    version            BIGINT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL
);
