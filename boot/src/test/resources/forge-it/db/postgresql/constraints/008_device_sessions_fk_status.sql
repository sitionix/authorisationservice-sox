ALTER TABLE device_sessions
    ADD CONSTRAINT device_sessions_fk_status
        FOREIGN KEY (status_id) REFERENCES session_statuses(id);
