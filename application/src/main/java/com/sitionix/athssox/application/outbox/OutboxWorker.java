package com.sitionix.athssox.application.outbox;

import com.sitionix.forge.outbox.core.service.OutboxDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxDispatcher outboxDispatcher;

    public void dispatchPendingEvents() {
        this.outboxDispatcher.dispatchPendingEvents();
    }
}
