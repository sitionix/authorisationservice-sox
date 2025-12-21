INSERT INTO outbox_aggregate_types (id, description)
VALUES (1, 'USER'),
       (2, 'SESSION')
ON CONFLICT (id) DO NOTHING;
