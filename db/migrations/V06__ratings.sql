-- V06: ratings, post-completion feedback between the two parties (FR-09).
-- Brief section 3.2 "Rating": Trip ID, reviewer, reviewed party, score, comment, timestamp.
-- The trip is identified by its match, since a match is what a completed trip is created from.

CREATE TABLE ratings (
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    match_id   UUID NOT NULL REFERENCES matches (id),
    rater_id   UUID NOT NULL REFERENCES users (id),
    ratee_id   UUID NOT NULL REFERENCES users (id),
    -- A score outside 1-5 would skew the admin metrics (FR-11) with no visible cause,
    -- since an average has no way to signal that one of its inputs was impossible.
    score      SMALLINT NOT NULL
               CONSTRAINT ratings_score_range_check CHECK (score BETWEEN 1 AND 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Each party rates the other exactly once per job, so a dissatisfied user cannot
    -- move an average by submitting repeatedly.
    CONSTRAINT ratings_match_rater_unique UNIQUE (match_id, rater_id),
    CONSTRAINT ratings_distinct_parties_check CHECK (rater_id <> ratee_id)
);

CREATE INDEX ratings_ratee_id_idx ON ratings (ratee_id);

COMMENT ON TABLE ratings IS
    'Two-way feedback per match. A user''s aggregate rating is derived from these rows, never stored.';
