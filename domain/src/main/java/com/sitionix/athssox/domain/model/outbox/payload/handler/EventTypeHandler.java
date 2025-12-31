package com.sitionix.athssox.domain.model.outbox.payload.handler;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;

public interface EventTypeHandler {

   <P> void doHandle(OutboxEvent<P> event);

   <P> P getPayload(String payload);

}
