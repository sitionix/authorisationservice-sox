CREATE TABLE IF NOT EXISTS session_statuses (
    id BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO session_statuses (id, description)
VALUES (1, 'ACTIVE'),
       (2, 'SUSPICIOUS'),
       (3, 'REVOKED BY USER'),
       (4, 'REVOKED BY ADMIN')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
