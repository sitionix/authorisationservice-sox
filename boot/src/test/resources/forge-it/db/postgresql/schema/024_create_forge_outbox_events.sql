CREATE TABLE IF NOT EXISTS forge_outbox_events (
    id                BIGSERIAL PRIMARY KEY,
    event_type        VARCHAR(255) NOT NULL,
    payload           TEXT         NOT NULL,
    idempotency_id    UUID         NOT NULL,
    trace_id          VARCHAR(255),
    headers           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    aggregate_type_id BIGINT,
    aggregate_id      BIGINT,
    initiator_type    VARCHAR(255),
    initiator_id      VARCHAR(255),
    status_id         BIGINT       NOT NULL,
    retry_count       INT          NOT NULL,
    next_retry_at     TIMESTAMPTZ  NOT NULL,
    last_error        TEXT,
    lock_until        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_forge_outbox_events_aggregate_type_id
        FOREIGN KEY (aggregate_type_id) REFERENCES forge_outbox_aggregate_types (id),
    CONSTRAINT fk_forge_outbox_events_status_id
        FOREIGN KEY (status_id) REFERENCES forge_outbox_statuses (id)
);
