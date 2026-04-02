CREATE TABLE IF NOT EXISTS device_sessions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_source_id VARCHAR(255) NOT NULL,
    status_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    initial_user_agent TEXT,
    last_user_agent TEXT,
    CONSTRAINT device_sessions_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT device_sessions_fk_status
        FOREIGN KEY (status_id) REFERENCES session_statuses(id),
    CONSTRAINT device_sessions_unique_source
        UNIQUE (user_id, session_source_id)
);

CREATE INDEX IF NOT EXISTS device_sessions_user_status_idx
    ON device_sessions (user_id, status_id);

CREATE INDEX IF NOT EXISTS device_sessions_source_idx
    ON device_sessions (session_source_id);
