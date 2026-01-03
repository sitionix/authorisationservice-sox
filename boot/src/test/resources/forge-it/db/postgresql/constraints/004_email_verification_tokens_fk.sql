ALTER TABLE email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_fk_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE email_verification_tokens
    ADD CONSTRAINT email_verification_tokens_fk_status
        FOREIGN KEY (status_id) REFERENCES email_verification_token_statuses(id);
