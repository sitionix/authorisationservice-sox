CREATE TABLE IF NOT EXISTS device_sessions (
    id                UUID PRIMARY KEY,
    user_id           BIGINT NOT NULL,
    session_source_id VARCHAR(255) NOT NULL,
    status_id         BIGINT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL,
    last_used_at      TIMESTAMPTZ NOT NULL,
    initial_user_agent TEXT,
    last_user_agent   TEXT
);
