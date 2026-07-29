-- V01: users, the owner record every other table hangs off.
-- Brief section 3.2 "User": ID, name, email, role, verification/compliance status, rating.
-- The aggregate rating is deliberately NOT stored here; it is derived from the ratings
-- table (V6) so it can never drift out of step with the rows it summarises.

CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT uuidv7(),
    full_name         TEXT NOT NULL CHECK (length(trim(full_name)) > 0),
    -- CITEXT compares case-insensitively, so one address cannot be registered twice
    -- under different capitalisation and then treated as two separate accounts.
    email             CITEXT NOT NULL UNIQUE,
    -- Brief section 2.2: "never store plain-text passwords". There is no plaintext
    -- column at any point; #9 writes a bcrypt/argon2 digest here.
    password_hash     TEXT NOT NULL,
    role              TEXT NOT NULL
                      CHECK (role IN ('FREIGHT_OWNER', 'TRANSPORTER', 'ADMIN')),
    compliance_status TEXT NOT NULL DEFAULT 'PENDING'
                      CHECK (compliance_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE users IS
    'Freight Owners, Transporters and Admins. Aggregate rating is derived from ratings, not stored.';


