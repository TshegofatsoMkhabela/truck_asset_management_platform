-- V04: matches, which truck was proposed for which load, and what was decided.
-- Brief section 3.2 "Match": ID, load, truck, score/reasons, status, created date.

CREATE TABLE matches (
    id         UUID PRIMARY KEY DEFAULT uuidv7(),
    load_id    UUID NOT NULL REFERENCES loads (id),
    truck_id   UUID NOT NULL REFERENCES trucks (id),
    -- Brief section 3.1: "The result should explain why the match was recommended."
    -- The score is the transparent eligibility number; reasons is the human-readable
    -- explanation behind it, held as JSON because the rule set that produces it (#13)
    -- will grow without the schema needing to change each time.
    score      NUMERIC(5, 2) NOT NULL
               CONSTRAINT matches_score_range_check CHECK (score >= 0 AND score <= 100),
    reasons    JSONB NOT NULL DEFAULT '[]'::jsonb,
    status     TEXT NOT NULL DEFAULT 'PROPOSED'
               CHECK (status IN ('PROPOSED', 'ACCEPTED', 'REJECTED')),
    decided_at TIMESTAMPTZ,
    decided_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Re-running the matcher must not create a second proposal for a pair it already
    -- proposed; without this, one agreement could produce two receipts (FR-07).
    CONSTRAINT matches_load_truck_unique UNIQUE (load_id, truck_id),

    -- A decided match with no timestamp, or an undecided match carrying one, is a state
    -- the audit trail (FR-12) cannot explain. The rubric grades "reliable state
    -- transitions", so the database refuses the inconsistent combinations outright.
    CONSTRAINT matches_decision_consistency_check CHECK (
        (status = 'PROPOSED' AND decided_at IS NULL AND decided_by IS NULL)
        OR (status <> 'PROPOSED' AND decided_at IS NOT NULL AND decided_by IS NOT NULL)
    )
);

CREATE INDEX matches_load_id_idx ON matches (load_id);
CREATE INDEX matches_truck_id_idx ON matches (truck_id);

COMMENT ON TABLE matches IS
    'Rule-based match proposals and their accept/reject outcome. One row per load/truck pair.';
