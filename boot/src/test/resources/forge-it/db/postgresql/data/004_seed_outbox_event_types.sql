INSERT INTO outbox_event_types (id, description)
VALUES (1, 'EMAIL_VERIFY'),
       (2, 'PASSWORD_RESET')
ON CONFLICT (id) DO NOTHING;
