-- V03: loads, a freight owner's cargo, the other half of what matching compares.
-- Brief section 3.2 "Load": ID, owner, origin, destination, cargo type, weight, volume,
-- pickup window, status.

CREATE TABLE loads (
    id                  UUID PRIMARY KEY DEFAULT uuidv7(),
    owner_id            UUID NOT NULL REFERENCES users (id),
    -- Location is matched by city name. FR-05 asks for location compatibility, and brief
    -- section 3.1 explicitly allows "matching cities/areas" instead of a distance value,
    -- so no geospatial extension is introduced.
    origin_city         TEXT NOT NULL CHECK (length(trim(origin_city)) > 0),
    destination_city    TEXT NOT NULL CHECK (length(trim(destination_city)) > 0),
    cargo_type          TEXT NOT NULL
                        CHECK (cargo_type IN ('GENERAL', 'REFRIGERATED', 'HAZARDOUS',
                                              'LIQUID', 'CONTAINER', 'BULK')),
    weight_kg           NUMERIC(10, 2) NOT NULL CONSTRAINT loads_weight_kg_check CHECK (weight_kg > 0),
    volume_m3           NUMERIC(10, 2) NOT NULL CONSTRAINT loads_volume_m3_check CHECK (volume_m3 > 0),
    pickup_window_start TIMESTAMPTZ NOT NULL,
    pickup_window_end   TIMESTAMPTZ NOT NULL,
    status              TEXT NOT NULL DEFAULT 'OPEN'
                        CHECK (status IN ('OPEN', 'MATCHED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT loads_pickup_window_check CHECK (pickup_window_end > pickup_window_start)
);

CREATE INDEX loads_owner_id_idx ON loads (owner_id);

COMMENT ON TABLE loads IS
    'Freight-owner cargo postings. Location is city-level only; proximity matching is not supported.';
