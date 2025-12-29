package com.sitionix.athssox.domain.command;

import com.sitionix.athssox.domain.model.outbox.OutboxEvent;

public interface OutboxCommand<P> {
    void execute(OutboxEvent<P> payload);
}
