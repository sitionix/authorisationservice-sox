CREATE INDEX IF NOT EXISTS idx_users_email_site_id
    ON users (email, site_id);

CREATE INDEX IF NOT EXISTS idx_users_email_global
    ON users (email)
    WHERE site_id IS NULL;
