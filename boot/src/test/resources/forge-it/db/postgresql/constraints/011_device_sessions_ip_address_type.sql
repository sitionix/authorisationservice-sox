ALTER TABLE device_sessions
    ALTER COLUMN initial_ip_address TYPE VARCHAR(64)
    USING initial_ip_address::text;
