CREATE TABLE IF NOT EXISTS refresh_token_statuses (
    id BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO refresh_token_statuses (id, description)
VALUES (1, 'ACTIVE'),
       (2, 'REVOKED')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
