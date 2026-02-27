CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_status
    ON forge_outbox_events (status);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_aggregate
    ON forge_outbox_events (aggregate_type, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_event_type
    ON forge_outbox_events (event_type);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator_type
    ON forge_outbox_events (initiator_type);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator_id
    ON forge_outbox_events (initiator_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator
    ON forge_outbox_events (initiator_type, initiator_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_polling
    ON forge_outbox_events (status, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_lock_until
    ON forge_outbox_events (lock_until);
