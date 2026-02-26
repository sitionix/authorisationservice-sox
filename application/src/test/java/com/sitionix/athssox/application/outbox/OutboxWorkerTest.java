package com.sitionix.athssox.application.outbox;

import com.sitionix.forge.outbox.core.service.OutboxDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    private OutboxWorker outboxWorker;

    @Mock
    private OutboxDispatcher outboxDispatcher;

    @BeforeEach
    void setUp() {
        this.outboxWorker = new OutboxWorker(this.outboxDispatcher);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.outboxDispatcher);
    }

    @Test
    void givenDispatcherBean_whenDispatchPendingEvents_thenDelegateToForgeOutboxDispatcher() {
        //given

        //when
        this.outboxWorker.dispatchPendingEvents();

        //then
        verify(this.outboxDispatcher).dispatchPendingEvents();
    }
}
