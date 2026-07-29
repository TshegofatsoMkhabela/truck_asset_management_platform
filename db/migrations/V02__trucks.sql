-- V02: trucks, a transporter's available capacity, one half of what matching compares.
-- Brief section 3.2 "Truck": ID, transporter, type, capacity, location, availability window, status.

CREATE TABLE trucks (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    transporter_id  UUID NOT NULL REFERENCES users (id),
    -- Vehicle type is a matching input, not decoration: FR-05 requires cargo/vehicle
    -- compatibility, which #13 evaluates against the load's cargo_type.
    vehicle_type    TEXT NOT NULL
                    CHECK (vehicle_type IN ('FLATBED', 'CURTAIN_SIDE', 'REFRIGERATED',
                                            'TANKER', 'CONTAINER', 'TIPPER')),
    -- A non-positive capacity would make every load fit (weight <= capacity is trivially
    -- true against a negative), so matching would silently recommend an impossible truck.
    capacity_kg     NUMERIC(10, 2) NOT NULL CONSTRAINT trucks_capacity_kg_check CHECK (capacity_kg > 0),
    -- Loads carry a volume, so trucks need something to compare it against or the load's
    -- volume can never take part in matching. Nullable on purpose: a flatbed has no
    -- enclosed volume, and null reads as "no volume constraint", not "unknown".
    capacity_m3     NUMERIC(10, 2) CONSTRAINT trucks_capacity_m3_check CHECK (capacity_m3 > 0),
    current_city    TEXT NOT NULL CHECK (length(trim(current_city)) > 0),
    available_from  TIMESTAMPTZ NOT NULL,
    available_until TIMESTAMPTZ NOT NULL,
    status          TEXT NOT NULL DEFAULT 'AVAILABLE'
                    CHECK (status IN ('AVAILABLE', 'MATCHED', 'UNAVAILABLE')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT trucks_availability_window_check CHECK (available_until > available_from)
);

-- PostgreSQL does not index a foreign key automatically, and "list my trucks" is the
-- transporter's most frequent read (FR-04).
CREATE INDEX trucks_transporter_id_idx ON trucks (transporter_id);

COMMENT ON TABLE trucks IS
    'Transporter-owned capacity. transporter_id is only constrained to be a real user; that the '
    'user holds the TRANSPORTER role is enforced in the service layer (#12).';
