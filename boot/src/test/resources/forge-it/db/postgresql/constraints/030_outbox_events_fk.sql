ALTER TABLE outbox_events
    ADD CONSTRAINT fk_outbox_events_aggregate_type
        FOREIGN KEY (aggregate_type_id) REFERENCES outbox_aggregate_types (id);

ALTER TABLE outbox_events
    ADD CONSTRAINT fk_outbox_events_event_type
        FOREIGN KEY (event_type_id) REFERENCES outbox_event_types (id);

ALTER TABLE outbox_events
    ADD CONSTRAINT fk_outbox_events_status
        FOREIGN KEY (status_id) REFERENCES outbox_statuses (id);
