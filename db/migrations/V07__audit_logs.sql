-- V07: audit_logs, the trail of key actions (FR-12).
-- Brief section 3.2 "Audit Event": Actor, action, entity, timestamp and relevant metadata.
--
-- This table deliberately breaks the referential-integrity rule every other table follows.
-- An audit trail must be MORE durable than the data it describes: a foreign key would mean
-- either a cascade delete erases the evidence, or a restrict blocks a legitimate deletion.
-- Both are wrong for a record whose purpose is to outlive the thing it records.

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY DEFAULT uuidv7(),
    -- No REFERENCES on actor_id or entity_id, by design (see above).
    actor_id    UUID,
    action      TEXT NOT NULL CHECK (length(trim(action)) > 0),
    entity_type TEXT NOT NULL CHECK (length(trim(entity_type)) > 0),
    entity_id   UUID,
    -- The variable part of an event. Kept as JSON so a new audited action in #10-#17 does
    -- not need a schema change; the four columns above stay real columns so the common
    -- questions ("who did what, to what, when") never require scanning a JSON document.
    details     JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX audit_logs_entity_idx ON audit_logs (entity_type, entity_id);
CREATE INDEX audit_logs_actor_id_idx ON audit_logs (actor_id);

-- Append-only enforcement. Without this, "audit trail" is a naming convention: any code
-- path with a database connection could rewrite history, and the rewrite would itself
-- leave no trace. A trigger is used rather than revoking privileges because the demo
-- runs as a single database user, so privilege separation would have nothing to separate.
CREATE FUNCTION audit_logs_reject_mutation() RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only: % is not permitted', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_append_only
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION audit_logs_reject_mutation();

COMMENT ON TABLE audit_logs IS
    'Append-only record of key actions. Intentionally has no foreign keys so entries survive '
    'deletion of the user or entity they describe.';
