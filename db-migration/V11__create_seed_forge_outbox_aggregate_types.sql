CREATE TABLE IF NOT EXISTS forge_outbox_aggregate_types (
    id BIGINT PRIMARY KEY,
    description VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO forge_outbox_aggregate_types (id, description)
VALUES (1, 'USER')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
