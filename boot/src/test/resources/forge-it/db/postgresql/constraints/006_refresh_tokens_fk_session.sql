ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_session
        FOREIGN KEY (session_id) REFERENCES device_sessions(id) ON DELETE CASCADE;
