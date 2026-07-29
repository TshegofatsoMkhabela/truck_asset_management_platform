-- V08: tracking_events, simulated trip progress for an accepted match (FR-08).
-- Brief section 3.2 "Tracking Event": Match/trip ID, latitude, longitude or route status,
-- timestamp. The "or" is load-bearing: either a position or a status is enough.

CREATE TABLE tracking_events (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    match_id    UUID NOT NULL REFERENCES matches (id),
    -- Plain numeric coordinates, not a geospatial type: brief section 4.1 excludes live
    -- telematics and production maps, and nothing here computes distance.
    latitude    NUMERIC(9, 6)
                CONSTRAINT tracking_events_latitude_check CHECK (latitude BETWEEN -90 AND 90),
    longitude   NUMERIC(9, 6)
                CONSTRAINT tracking_events_longitude_check CHECK (longitude BETWEEN -180 AND 180),
    status      TEXT CHECK (status IN ('DISPATCHED', 'IN_TRANSIT', 'ARRIVED', 'DELIVERED')),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- An event carrying neither a position nor a status says nothing happened, which is
    -- not an event. Rejecting it here stops #15 from rendering blank rows on a trip view.
    CONSTRAINT tracking_events_position_or_status_check CHECK (
        status IS NOT NULL OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    )
);

CREATE INDEX tracking_events_match_id_idx ON tracking_events (match_id, occurred_at);

COMMENT ON TABLE tracking_events IS
    'Mock trip progress. Coordinates are synthetic; no live GPS source exists or is planned.';
