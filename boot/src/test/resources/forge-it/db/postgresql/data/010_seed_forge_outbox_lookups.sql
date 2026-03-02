INSERT INTO forge_outbox_statuses (id, description)
VALUES (1, 'PENDING'),
       (2, 'IN_PROGRESS'),
       (3, 'SENT'),
       (4, 'FAILED'),
       (5, 'DEAD')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO forge_outbox_aggregate_types (id, description)
VALUES (1, 'USER')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;

INSERT INTO forge_outbox_event_types (id, description)
VALUES (1, 'EMAIL_VERIFY')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;

SELECT setval(
        pg_get_serial_sequence('forge_outbox_event_types', 'id'),
        COALESCE((SELECT MAX(id) FROM forge_outbox_event_types), 1),
        true
       );
