INSERT INTO outbox_statuses (id, description)
VALUES (1, 'PENDING'),
       (2, 'IN_PROGRESS'),
       (3, 'SENT'),
       (4, 'FAILED'),
       (5, 'DEAD')
ON CONFLICT (id) DO NOTHING;
