ALTER TABLE forge_outbox_events
    ADD COLUMN IF NOT EXISTS idempotency_id UUID NOT NULL,
    ADD COLUMN IF NOT EXISTS headers JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS initiator_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS initiator_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_idempotency_id
    ON forge_outbox_events (idempotency_id);
