package com.sitionix.athssox.domain.model.outbox.payload.handler;

public interface EventTypeHandler {

   <T> T getPayload(String payload);
}
