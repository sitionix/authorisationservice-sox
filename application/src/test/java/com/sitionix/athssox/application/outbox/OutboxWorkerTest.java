package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.domain.config.OutboxWorkerConfig;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.model.outbox.payload.handler.EventTypeHandler;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    private OutboxWorker outboxWorker;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private OutboxWorkerConfig outboxWorkerConfig;

    @Mock
    private EventTypeHandler<Object> eventTypeHandler;

    @BeforeEach
    void setUp() {
        OutboxEventType.EMAIL_VERIFY.setHandler(this.eventTypeHandler);
        this.outboxWorker = new OutboxWorker(this.outboxEventRepository, this.outboxWorkerConfig);
    }

    @AfterEach
    void tearDown() {
        OutboxEventType.EMAIL_VERIFY.setHandler(null);
        verifyNoMoreInteractions(this.outboxEventRepository,
                this.outboxWorkerConfig,
                this.eventTypeHandler);
    }

    @Test
    void given_no_events_when_dispatch_pending_events_then_skip_processing() {
        //given
        final int batchSize = 10;
        final List<String> statuses = this.getEventStatuses();
        final List<String> eventTypes = this.getEventTypes();

        when(this.outboxWorkerConfig.getBatchSize())
                .thenReturn(batchSize);
        when(this.outboxEventRepository.claimPendingEvents(eq(statuses),
                eq(eventTypes),
                eq(batchSize),
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        //when
        this.outboxWorker.dispatchPendingEvents();

        //then
        verify(this.outboxWorkerConfig)
                .getBatchSize();
        verify(this.outboxEventRepository)
                .claimPendingEvents(eq(statuses),
                        eq(eventTypes),
                        eq(batchSize),
                        any(LocalDateTime.class));
    }

    @Test
    void given_pending_event_when_dispatch_pending_events_then_handle_and_mark_sent() {
        //given
        final int batchSize = 1;
        final OutboxEvent<Object> event = this.getOutboxEvent(1L);
        final List<String> statuses = this.getEventStatuses();
        final List<String> eventTypes = this.getEventTypes();

        when(this.outboxWorkerConfig.getBatchSize())
                .thenReturn(batchSize);
        when(this.outboxEventRepository.claimPendingEvents(eq(statuses),
                eq(eventTypes),
                eq(batchSize),
                any(LocalDateTime.class)))
                .thenReturn(List.of(event));

        //when
        this.outboxWorker.dispatchPendingEvents();

        //then
        verify(this.outboxWorkerConfig).getBatchSize();
        verify(this.eventTypeHandler).doHandle(event);
        verify(this.outboxEventRepository).claimPendingEvents(eq(statuses),
                        eq(eventTypes),
                        eq(batchSize),
                        any(LocalDateTime.class));

        verify(this.outboxEventRepository).markSent(1L);
    }

    @Test
    void given_handler_error_when_dispatch_pending_events_then_mark_failed() {
        //given
        final int batchSize = 1;
        final long retryDelaySeconds = 30L;
        final int maxRetries = 3;
        final OutboxEvent<Object> event = this.getOutboxEvent(5L);
        final List<String> statuses = this.getEventStatuses();
        final List<String> eventTypes = this.getEventTypes();
        final RuntimeException exception = new RuntimeException("boom");

        when(this.outboxWorkerConfig.getBatchSize())
                .thenReturn(batchSize);
        when(this.outboxWorkerConfig.getRetryDelaySeconds())
                .thenReturn(retryDelaySeconds);
        when(this.outboxWorkerConfig.getMaxRetries())
                .thenReturn(maxRetries);
        when(this.outboxEventRepository.claimPendingEvents(eq(statuses),
                eq(eventTypes),
                eq(batchSize),
                any(LocalDateTime.class)))
                .thenReturn(List.of(event));
        doThrow(exception)
                .when(this.eventTypeHandler)
                .doHandle(event);

        //when
        this.outboxWorker.dispatchPendingEvents();

        //then
        verify(this.outboxWorkerConfig).getBatchSize();
        verify(this.outboxWorkerConfig).getRetryDelaySeconds();
        verify(this.outboxWorkerConfig).getMaxRetries();
        verify(this.eventTypeHandler).doHandle(event);
        verify(this.outboxEventRepository).claimPendingEvents(eq(statuses),
                        eq(eventTypes),
                        eq(batchSize),
                        any(LocalDateTime.class));
        verify(this.outboxEventRepository).markFailed(eq(5L),
                        eq("boom"),
                        eq(Duration.ofSeconds(retryDelaySeconds)),
                        eq(maxRetries));
    }

    private List<String> getEventStatuses() {
        return List.of(OutboxStatus.PENDING.getDescription(),
                OutboxStatus.FAILED.getDescription());
    }

    private List<String> getEventTypes() {
        return List.of(OutboxEventType.EMAIL_VERIFY.getDescription());
    }

    private OutboxEvent<Object> getOutboxEvent(final Long id) {
        return OutboxEvent.<Object>builder()
                .id(id)
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .build();
    }
}
