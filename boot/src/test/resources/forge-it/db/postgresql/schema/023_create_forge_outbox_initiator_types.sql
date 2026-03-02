CREATE TABLE IF NOT EXISTS forge_outbox_initiator_types (
    id          BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL UNIQUE
);
