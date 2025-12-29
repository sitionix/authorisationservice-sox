package com.sitionix.athssox.domain.event;

public interface EventHandler<P> {

    void publish(P payload);
}
