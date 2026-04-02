CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    session_id UUID NOT NULL,
    status_id BIGINT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    rotated_from_token_id UUID,
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(64),
    CONSTRAINT refresh_tokens_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT refresh_tokens_fk_session
        FOREIGN KEY (session_id) REFERENCES device_sessions(id) ON DELETE CASCADE,
    CONSTRAINT refresh_tokens_fk_status
        FOREIGN KEY (status_id) REFERENCES refresh_token_statuses(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);

CREATE INDEX IF NOT EXISTS refresh_tokens_session_status_idx
    ON refresh_tokens (session_id, status_id);
