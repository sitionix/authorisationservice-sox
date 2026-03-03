CREATE TABLE IF NOT EXISTS forge_outbox_statuses (
    id          BIGINT PRIMARY KEY,
    description VARCHAR(32) NOT NULL UNIQUE
);

INSERT INTO forge_outbox_statuses (id, description)
VALUES (1, 'PENDING'),
       (2, 'IN_PROGRESS'),
       (3, 'SENT'),
       (4, 'FAILED'),
       (5, 'DEAD')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
