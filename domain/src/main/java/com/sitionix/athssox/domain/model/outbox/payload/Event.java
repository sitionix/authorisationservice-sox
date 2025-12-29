package com.sitionix.athssox.domain.model.outbox.payload;

import com.sitionix.athssox.domain.event.DomainEvent;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Event<T> implements DomainEvent<T> {

    private final T payload;
    private final UUID idempotencyId;
    private final Instant createdAt;
    private final String user;
    private final String eventType;

    public Event(final T payload,
                 final String user,
                 final String eventType,
                 final Instant createdAt) {
        this.payload = payload;
        this.idempotencyId = UUID.randomUUID();
        this.createdAt = createdAt;
        this.user = user;
        this.eventType = eventType;
    }
}
