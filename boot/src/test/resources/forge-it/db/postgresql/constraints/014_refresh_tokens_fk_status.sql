ALTER TABLE refresh_tokens
    ADD CONSTRAINT refresh_tokens_fk_status
        FOREIGN KEY (status_id) REFERENCES refresh_token_statuses(id);
