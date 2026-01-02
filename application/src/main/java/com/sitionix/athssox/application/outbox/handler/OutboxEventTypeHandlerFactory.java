package com.sitionix.athssox.application.outbox.handler;

import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class OutboxEventTypeHandlerFactory {

    private final ApplicationContext applicationContext;

    @PostConstruct
    public void registerHandlers() {

        Arrays.stream(OutboxEventType.values())
                .forEach(value -> {
                    final EventTypeHandler handler = this.applicationContext.getBean(
                            value.getServiceName(),
                            EventTypeHandler.class);
                    value.setHandler(handler);
                });
    }
}
