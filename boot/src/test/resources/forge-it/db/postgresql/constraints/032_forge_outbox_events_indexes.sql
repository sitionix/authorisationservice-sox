CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_status_id
    ON forge_outbox_events (status_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_aggregate
    ON forge_outbox_events (aggregate_type_id, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_event_type_id
    ON forge_outbox_events (event_type_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator_type_id
    ON forge_outbox_events (initiator_type_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator_id
    ON forge_outbox_events (initiator_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_initiator
    ON forge_outbox_events (initiator_type_id, initiator_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_polling
    ON forge_outbox_events (status_id, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_lock_until
    ON forge_outbox_events (lock_until);
