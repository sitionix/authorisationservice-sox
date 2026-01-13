ALTER TABLE device_sessions
    ADD CONSTRAINT device_sessions_unique_source UNIQUE (user_id, session_source_id);

CREATE INDEX IF NOT EXISTS device_sessions_user_status_idx
    ON device_sessions (user_id, status_id);

CREATE INDEX IF NOT EXISTS device_sessions_source_idx
    ON device_sessions (session_source_id);
