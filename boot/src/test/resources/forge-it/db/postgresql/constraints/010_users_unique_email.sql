-- Conditional uniqueness:
-- - Site-scoped roles (e.g., SITE USER, SITE ADMIN) enforce uniqueness per site.
-- - Global roles (e.g., SUPER ADMIN, ECOSYSTEM OWNER) enforce global uniqueness.

CREATE UNIQUE INDEX uq_users_email_per_site_role
    ON users (email, site_id)
    WHERE role_id IN (1, 4);

CREATE UNIQUE INDEX uq_users_email_global_role
    ON users (email)
    WHERE role_id IN (2, 3);
