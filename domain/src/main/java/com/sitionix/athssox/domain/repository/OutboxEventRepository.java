package com.sitionix.athssox.domain.repository;

import com.sitionix.athssox.domain.model.outbox.OutboxEventCreate;

public interface OutboxEventRepository {

    void create(final OutboxEventCreate outboxEventCreate);
}
