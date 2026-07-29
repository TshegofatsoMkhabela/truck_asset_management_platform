-- V09: disputes, flagged items an admin resolves (FR-10).
-- Brief section 2.2 requires "create/view a basic dispute or flag"; there is no dispute
-- object in section 3.2, so the shape here is the minimum that supports the admin console.

CREATE TABLE disputes (
    id              UUID PRIMARY KEY DEFAULT uuidv7(),
    -- A dispute is about a match, a flag is about a user, and FR-10 covers both as
    -- "flagged/disputed items". Either column may be null, but not both: a complaint about
    -- nothing in particular is not actionable by an admin.
    match_id        UUID REFERENCES matches (id),
    subject_user_id UUID REFERENCES users (id),
    raised_by       UUID NOT NULL REFERENCES users (id),
    description     TEXT NOT NULL CHECK (length(trim(description)) > 0),
    status          TEXT NOT NULL DEFAULT 'OPEN'
                    CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'REJECTED')),
    resolution_note TEXT,
    resolved_by     UUID REFERENCES users (id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,

    -- Same reasoning as matches: a closed dispute that cannot say when or by whom it was
    -- closed is a hole in the admin audit story (FR-10, FR-12).
    CONSTRAINT disputes_subject_check CHECK (
        match_id IS NOT NULL OR subject_user_id IS NOT NULL
    ),

    CONSTRAINT disputes_resolution_consistency_check CHECK (
        (status IN ('OPEN', 'UNDER_REVIEW') AND resolved_at IS NULL AND resolved_by IS NULL)
        OR (status IN ('RESOLVED', 'REJECTED') AND resolved_at IS NOT NULL AND resolved_by IS NOT NULL)
    )
);

CREATE INDEX disputes_status_idx ON disputes (status);

COMMENT ON TABLE disputes IS
    'Flags raised against a match, resolved by an Admin. Open disputes drive the admin console queue.';
