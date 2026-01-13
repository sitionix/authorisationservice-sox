INSERT INTO session_statuses (id, description)
VALUES (1, 'ACTIVE'),
       (2, 'SUSPICIOUS'),
       (3, 'REVOKED BY USER'),
       (4, 'REVOKED BY ADMIN')
ON CONFLICT (id) DO NOTHING;
