package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.domain.config.OutboxWorkerConfig;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxWorkerConfig config;

    @Scheduled(fixedDelayString = "#{@outboxWorkerConfig.pollDelayMs}")
    public void dispatchPendingEvents() {
        final List<OutboxEvent<Object>> events = this.outboxEventRepository.claimPendingEvents(
                this.supportedEventStatuses(),
                this.supportedEventTypes(),
                this.config.getBatchSize(),
                LocalDateTime.now());

        if (events.isEmpty()) {
            return;
        }

        events.forEach(this::handleEvent);
    }

    private void handleEvent(final OutboxEvent<Object> event) {
        try {
            event.getEventType().doHandle(event);
            this.outboxEventRepository.markSent(event.getId());
        } catch (final Exception exception) {
            log.warn("Failed to publish outbox event id={}", event.getId(), exception);
            this.outboxEventRepository.markFailed(
                    event.getId(),
                    this.formatErrorMessage(exception),
                    Duration.ofSeconds(this.config.getRetryDelaySeconds()),
                    this.config.getMaxRetries());
        }
    }

    private List<String> supportedEventTypes() {
        return Stream.of(OutboxEventType.values())
                .map(OutboxEventType::getDescription)
                .toList();
    }

    private List<String> supportedEventStatuses() {
        return Stream.of(OutboxStatus.PENDING, OutboxStatus.FAILED)
                .map(OutboxStatus::getDescription)
                .toList();
    }

    private String formatErrorMessage(final Exception exception) {
        final String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
