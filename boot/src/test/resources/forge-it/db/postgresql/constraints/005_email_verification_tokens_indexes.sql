CREATE UNIQUE INDEX IF NOT EXISTS email_verification_tokens_unique_token_hash
    ON email_verification_tokens (token_hash);

CREATE INDEX IF NOT EXISTS email_verification_tokens_user_expires_idx
    ON email_verification_tokens (user_id, expires_at);

CREATE INDEX IF NOT EXISTS email_verification_tokens_user_created_idx
    ON email_verification_tokens (user_id, created_at);
