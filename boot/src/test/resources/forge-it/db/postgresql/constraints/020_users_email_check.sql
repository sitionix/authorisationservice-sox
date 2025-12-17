ALTER TABLE users
    ADD CONSTRAINT chk_users_email CHECK (email ~* E'^[^@[:space:]]+@[^@[:space:]]+\\.[^@[:space:]]+$');
