CREATE INDEX IF NOT EXISTS refresh_tokens_session_status_idx
    ON refresh_tokens (session_id, status_id);
