CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_id BIGINT NOT NULL,
    status_id BIGINT NOT NULL,
    site_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_users_global_role
        FOREIGN KEY (role_id) REFERENCES global_roles (id),
    CONSTRAINT fk_users_status
        FOREIGN KEY (status_id) REFERENCES user_statuses (id),
    CONSTRAINT chk_users_email
        CHECK (email ~* E'^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_per_site_role
    ON users (email, site_id)
    WHERE role_id IN (1, 4);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_global_role
    ON users (email)
    WHERE role_id IN (2, 3);

CREATE INDEX IF NOT EXISTS idx_users_email_site_id
    ON users (email, site_id);

CREATE INDEX IF NOT EXISTS idx_users_email_global
    ON users (email)
    WHERE site_id IS NULL;
