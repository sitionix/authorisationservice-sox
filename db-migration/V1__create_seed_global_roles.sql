CREATE TABLE IF NOT EXISTS global_roles (
    id BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO global_roles (id, description)
VALUES (1, 'SITE USER'),
       (2, 'SUPER ADMIN'),
       (3, 'ECOSYSTEM OWNER'),
       (4, 'SITE ADMIN')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
