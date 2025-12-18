ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
