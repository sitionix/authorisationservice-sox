CREATE TABLE IF NOT EXISTS session_statuses (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);
