CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id         UUID PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    site_id    UUID,
    token_hash VARCHAR(128) NOT NULL,
    status_id  BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
