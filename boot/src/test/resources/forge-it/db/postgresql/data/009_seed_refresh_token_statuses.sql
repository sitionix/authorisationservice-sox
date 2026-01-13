INSERT INTO refresh_token_statuses (id, description)
VALUES (1, 'ACTIVE'),
       (2, 'REVOKED')
ON CONFLICT (id) DO NOTHING;
