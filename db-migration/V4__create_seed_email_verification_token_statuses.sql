CREATE TABLE IF NOT EXISTS email_verification_token_statuses (
    id BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO email_verification_token_statuses (id, description)
VALUES (1, 'ACTIVE'),
       (2, 'USED'),
       (3, 'REVOKED')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
