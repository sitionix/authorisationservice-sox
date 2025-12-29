CREATE TABLE IF NOT EXISTS outbox_statuses (
    id          BIGINT PRIMARY KEY,
    description TEXT NOT NULL
);
