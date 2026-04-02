CREATE TABLE IF NOT EXISTS user_statuses (
    id BIGINT PRIMARY KEY,
    description VARCHAR(64) NOT NULL UNIQUE
);

INSERT INTO user_statuses (id, description)
VALUES (1, 'PENDING EMAIL VERIFY'),
       (2, 'ACTIVE'),
       (3, 'INACTIVE'),
       (4, 'BANNED')
ON CONFLICT (id) DO UPDATE SET description = EXCLUDED.description;
