INSERT INTO user_statuses (id, description) VALUES
    (1, 'PENDING EMAIL VERIFY'),
    (2, 'ACTIVE'),
    (3, 'INACTIVE'),
    (4, 'BANNED')
ON CONFLICT (id) DO NOTHING;
