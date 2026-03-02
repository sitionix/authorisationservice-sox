CREATE TABLE IF NOT EXISTS forge_outbox_statuses (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(32) NOT NULL UNIQUE
);
