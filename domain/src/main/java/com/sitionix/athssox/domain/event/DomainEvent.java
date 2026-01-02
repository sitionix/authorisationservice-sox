package com.sitionix.athssox.domain.event;

import java.time.Instant;

public interface DomainEvent<P> {

    P getPayload();

    Instant getCreatedAt();

    String getEventType();
}
