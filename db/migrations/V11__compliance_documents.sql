-- V11: compliance_documents, the simulated verification paperwork behind a user's
-- compliance status (FR-02).
--
-- Brief section 2.2 asks for "mock compliance-document metadata and admin approval status".
-- users.compliance_status already carries the overall verdict; this table carries the
-- individual documents that verdict is based on, so an admin reviewing a transporter can
-- see which licence was approved and which is still outstanding.
--
-- Metadata only. No file content is stored and no upload actually happens: section 4.1
-- keeps real document handling out of scope, and FR-02 asks for "simulated document upload
-- or metadata".

CREATE TABLE compliance_documents (
    id            UUID PRIMARY KEY DEFAULT uuidv7(),
    user_id       UUID NOT NULL REFERENCES users (id),
    document_type TEXT NOT NULL
                  CHECK (document_type IN ('ID_DOCUMENT', 'OPERATOR_LICENCE', 'INSURANCE',
                                           'VEHICLE_REGISTRATION', 'TAX_CLEARANCE')),
    -- The name a reviewer sees in the admin console. Not a path: nothing is written to disk.
    file_name     TEXT NOT NULL CHECK (length(trim(file_name)) > 0),
    status        TEXT NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_by   UUID REFERENCES users (id),
    reviewed_at   TIMESTAMPTZ,

    -- Same rule as matches and disputes: a decided record must say who decided and when,
    -- or the audit trail (FR-12) cannot explain how a user came to be approved.
    CONSTRAINT compliance_documents_review_consistency_check CHECK (
        (status = 'PENDING' AND reviewed_at IS NULL AND reviewed_by IS NULL)
        OR (status <> 'PENDING' AND reviewed_at IS NOT NULL AND reviewed_by IS NOT NULL)
    )
);

CREATE INDEX compliance_documents_user_id_idx ON compliance_documents (user_id, status);

COMMENT ON TABLE compliance_documents IS
    'Simulated verification paperwork. Metadata only; no file content is stored anywhere.';
