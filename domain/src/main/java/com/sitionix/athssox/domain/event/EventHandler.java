package com.sitionix.athssox.domain.event;

import com.sitionix.athssox.domain.model.outbox.payload.Event;

public interface EventHandler<E> {

    void publish(Event<E> event);
}
