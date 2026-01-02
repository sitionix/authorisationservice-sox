CREATE INDEX IF NOT EXISTS idx_outbox_events_status_id
    ON outbox_events (status_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate
    ON outbox_events (aggregate_type_id, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type
    ON outbox_events (event_type_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_initiator_type
    ON outbox_events (initiator_type_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_initiator_id
    ON outbox_events (initiator_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_initiator
    ON outbox_events (initiator_type_id, initiator_id);

CREATE INDEX IF NOT EXISTS idx_outbox_events_polling
    ON outbox_events (status_id, next_retry_at, created_at);
