ALTER TABLE device_sessions
    ADD CONSTRAINT device_sessions_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
