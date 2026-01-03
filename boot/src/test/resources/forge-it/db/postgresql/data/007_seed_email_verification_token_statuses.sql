INSERT INTO email_verification_token_statuses (id, description) VALUES
    (1, 'ACTIVE'),
    (2, 'USED'),
    (3, 'REVOKED')
ON CONFLICT (id) DO NOTHING;
