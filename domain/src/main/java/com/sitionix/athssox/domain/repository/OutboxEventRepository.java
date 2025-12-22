package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;

public interface OutboxEventRepository {

    void create(final OutboxEvent<?> outboxEvent);
}
