CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    site_id UUID,
    token_hash VARCHAR(128) NOT NULL,
    status_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT email_verification_tokens_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT email_verification_tokens_fk_status
        FOREIGN KEY (status_id) REFERENCES email_verification_token_statuses(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS email_verification_tokens_unique_token_hash
    ON email_verification_tokens (token_hash);

CREATE INDEX IF NOT EXISTS email_verification_tokens_user_expires_idx
    ON email_verification_tokens (user_id, expires_at);

CREATE INDEX IF NOT EXISTS email_verification_tokens_user_created_idx
    ON email_verification_tokens (user_id, created_at);
