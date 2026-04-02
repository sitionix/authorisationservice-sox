CREATE TABLE IF NOT EXISTS forge_outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    trace_id VARCHAR(255),
    aggregate_type_id BIGINT,
    aggregate_id BIGINT,
    status_id BIGINT NOT NULL,
    retry_count INT NOT NULL,
    next_retry_at TIMESTAMPTZ NOT NULL,
    last_error TEXT,
    lock_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_forge_outbox_events_aggregate_type_id
        FOREIGN KEY (aggregate_type_id) REFERENCES forge_outbox_aggregate_types (id),
    CONSTRAINT fk_forge_outbox_events_status_id
        FOREIGN KEY (status_id) REFERENCES forge_outbox_statuses (id)
);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_status_id
    ON forge_outbox_events (status_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_aggregate
    ON forge_outbox_events (aggregate_type_id, aggregate_id);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_event_type
    ON forge_outbox_events (event_type);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_polling
    ON forge_outbox_events (status_id, next_retry_at, created_at);

CREATE INDEX IF NOT EXISTS idx_forge_outbox_events_lock_until
    ON forge_outbox_events (lock_until);
