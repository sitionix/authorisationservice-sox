package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.athssox.domain.event.DomainEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Event<T> implements DomainEvent<T> {

    private final T payload;
    private final UUID idempotencyId;
    private final Instant createdAt;
    private final String eventType;
    private final String id;

    private Event(final String id,
                  final T payload,
                  final String eventType,
                  final Instant createdAt) {
        this.id = id;
        this.payload = payload;
        this.idempotencyId = UUID.randomUUID();
        this.createdAt = createdAt;
        this.eventType = eventType;
    }

    public static <T> Event<T> create(final String id,
                                      final T payload,
                                      final String eventType,
                                      final Instant createdAt) {
        return new Event<>(id, payload, eventType, createdAt);
    }

    public static <T> Event<T> create(final OutboxEvent<T> event) {
        return Event.create(event.getId().toString(),
                event.getPayload(),
                event.getEventType().getDescription(),
                event.getCreatedAt());
    }
}
