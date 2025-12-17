CREATE TABLE IF NOT EXISTS user_statuses (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);
