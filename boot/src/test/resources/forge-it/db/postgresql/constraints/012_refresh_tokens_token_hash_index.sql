CREATE UNIQUE INDEX idx_refresh_tokens_token_hash
    ON refresh_tokens (token_hash);
