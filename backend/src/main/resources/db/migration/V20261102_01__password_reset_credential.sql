-- Additive and repeatable: creates only the reset table, never changes existing account rows.
-- Timestamps are stored as UTC. One row per student bounds retained reset state.
CREATE TABLE IF NOT EXISTS password_reset_credential (
    student_id BIGINT NOT NULL PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMP(3) NOT NULL,
    expires_at TIMESTAMP(3) NOT NULL,
    consumed_at TIMESTAMP(3) NULL
);
