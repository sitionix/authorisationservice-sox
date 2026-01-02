CREATE TABLE IF NOT EXISTS email_verification_token_statuses (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);
