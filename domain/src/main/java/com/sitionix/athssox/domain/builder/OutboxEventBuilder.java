package com.sitionix.athssox.domain.builder;

import com.sitionix.athssox.domain.model.outbox.OutboxBuildContext;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;

public interface OutboxEventBuilder<P> {

    OutboxEventType eventType();
    OutboxEvent<P> build(OutboxBuildContext ctx);

}
