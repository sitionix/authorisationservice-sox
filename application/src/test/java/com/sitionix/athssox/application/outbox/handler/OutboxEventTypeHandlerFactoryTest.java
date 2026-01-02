package com.sitionix.athssox.application.outbox.handler;

import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventTypeHandlerFactoryTest {

    private OutboxEventTypeHandlerFactory factory;

    @Mock
    private ApplicationContext applicationContext;

    private Map<OutboxEventType, EventTypeHandler<Object>> previousHandlers;

    private List<EventTypeHandler<Object>> handlerMocks;

    @BeforeEach
    void setUp() {
        this.factory = new OutboxEventTypeHandlerFactory(this.applicationContext);
        this.previousHandlers = new EnumMap<>(OutboxEventType.class);
        for (OutboxEventType eventType : OutboxEventType.values()) {
            this.previousHandlers.put(eventType, eventType.getHandler());
        }
        this.handlerMocks = List.of();
    }

    @AfterEach
    void tearDown() {
        for (Map.Entry<OutboxEventType, EventTypeHandler<Object>> entry : this.previousHandlers.entrySet()) {
            entry.getKey().setHandler(entry.getValue());
        }
        verifyNoMoreInteractions(this.applicationContext);
        for (EventTypeHandler<Object> handler : this.handlerMocks) {
            verifyNoMoreInteractions(handler);
        }
    }

    @Test
    void givenEventTypes_whenRegisterHandlers_thenAssignHandlers() {
        //given
        final Map<OutboxEventType, EventTypeHandler<Object>> handlersByType = this.getHandlersByType();

        for (Map.Entry<OutboxEventType, EventTypeHandler<Object>> entry : handlersByType.entrySet()) {
            when(this.applicationContext.getBean(entry.getKey().getServiceName(), EventTypeHandler.class))
                    .thenReturn(entry.getValue());
        }
        this.handlerMocks = this.getHandlerMocks(handlersByType);

        //when
        this.factory.registerHandlers();

        //then
        for (Map.Entry<OutboxEventType, EventTypeHandler<Object>> entry : handlersByType.entrySet()) {
            assertThat(entry.getKey().getHandler()).isEqualTo(entry.getValue());
            verify(this.applicationContext).getBean(entry.getKey().getServiceName(), EventTypeHandler.class);
        }
    }

    private Map<OutboxEventType, EventTypeHandler<Object>> getHandlersByType() {
        final Map<OutboxEventType, EventTypeHandler<Object>> handlers = new EnumMap<>(OutboxEventType.class);
        for (OutboxEventType eventType : OutboxEventType.values()) {
            handlers.put(eventType, mock(EventTypeHandler.class));
        }
        return handlers;
    }

    private List<EventTypeHandler<Object>> getHandlerMocks(final Map<OutboxEventType, EventTypeHandler<Object>> handlersByType) {
        return new ArrayList<>(handlersByType.values());
    }
}
