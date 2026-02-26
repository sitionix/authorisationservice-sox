package com.sitionix.athssox.application.outbox.storage;

import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import com.sitionix.forge.outbox.core.port.OutboxPayloadCodec;
import com.sitionix.forge.outbox.core.model.OutboxRecord;
import com.sitionix.forge.outbox.core.model.OutboxStatus;
import com.sitionix.forge.outbox.core.port.OutboxStorage;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AuthOutboxStorageAdapter implements OutboxStorage {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPayloadCodec outboxPayloadCodec;

    public AuthOutboxStorageAdapter(final OutboxEventRepository outboxEventRepository,
                                    final OutboxPayloadCodec outboxPayloadCodec) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxPayloadCodec = outboxPayloadCodec;
    }

    @Override
    public void enqueue(final OutboxRecord record) {
        final OutboxEvent<Object> outboxEvent = OutboxEvent.<Object>builder()
                .aggregateType(this.asAggregateType(record.getAggregateType()))
                .aggregateId(record.getAggregateId())
                .initiatorType(this.asInitiatorType(record.getInitiatorType()))
                .initiatorId(record.getInitiatorId())
                .eventType(this.asEventType(record.getEventType()))
                .status(com.sitionix.athssox.domain.model.outbox.OutboxStatus.PENDING)
                .retryCount(record.getAttempts())
                .nextRetryAt(LocalDateTime.ofInstant(record.getNextAttemptAt(), ZoneOffset.UTC))
                .payload(record.getPayload())
                .lastError(record.getLastError())
                .build();

        this.outboxEventRepository.create(outboxEvent);
    }

    @Override
    public List<OutboxRecord> claimPendingEvents(final Set<OutboxStatus> eventStatuses,
                                                 final Set<String> eventTypes,
                                                 final int batchSize,
                                                 final Instant now,
                                                 final boolean lockEnabled,
                                                 final Duration lockLease) {
        final List<String> statuses = eventStatuses.stream()
                .map(Enum::name)
                .toList();

        return this.outboxEventRepository.claimPendingEvents(
                        statuses,
                        List.copyOf(eventTypes),
                        batchSize,
                        LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                .stream()
                .map(this::toOutboxRecord)
                .toList();
    }

    @Override
    public void markSent(final String outboxEventId) {
        this.outboxEventRepository.markSent(Long.valueOf(outboxEventId));
    }

    @Override
    public void markFailed(final String outboxEventId,
                           final String errorMessage,
                           final Duration retryDelay,
                           final int maxRetries,
                           final Instant now) {
        this.outboxEventRepository.markFailed(
                Long.valueOf(outboxEventId),
                errorMessage,
                retryDelay,
                maxRetries);
    }

    private OutboxRecord toOutboxRecord(final OutboxEvent<Object> outboxEvent) {
        return OutboxRecord.builder()
                .id(String.valueOf(outboxEvent.getId()))
                .eventType(outboxEvent.getEventType().getDescription())
                .payload(this.asRawPayload(outboxEvent.getPayload()))
                .headers(Map.of())
                .metadata(Map.of())
                .traceId(null)
                .aggregateType(outboxEvent.getAggregateType().getDescription())
                .aggregateId(outboxEvent.getAggregateId())
                .initiatorType(outboxEvent.getInitiatorType() == null
                        ? InitiatorType.SYSTEM.getDescription()
                        : outboxEvent.getInitiatorType().getDescription())
                .initiatorId(outboxEvent.getInitiatorId())
                .status(OutboxStatus.valueOf(outboxEvent.getStatus().getDescription()))
                .attempts(outboxEvent.getRetryCount())
                .nextAttemptAt(outboxEvent.getNextRetryAt() == null
                        ? null
                        : outboxEvent.getNextRetryAt().toInstant(ZoneOffset.UTC))
                .lastError(outboxEvent.getLastError())
                .createdAt(outboxEvent.getCreatedAt())
                .updatedAt(null)
                .build();
    }

    private String asRawPayload(final Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof String rawPayload) {
            return rawPayload;
        }
        return this.outboxPayloadCodec.serialize(payload);
    }

    private OutboxAggregateType asAggregateType(final String description) {
        return Arrays.stream(OutboxAggregateType.values())
                .filter(value -> value.getDescription().equals(description))
                .findFirst()
                .orElse(OutboxAggregateType.USER);
    }

    private OutboxEventType asEventType(final String description) {
        return Arrays.stream(OutboxEventType.values())
                .filter(value -> value.getDescription().equals(description))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event type: " + description));
    }

    private InitiatorType asInitiatorType(final String description) {
        return Arrays.stream(InitiatorType.values())
                .filter(value -> value.getDescription().equals(description))
                .findFirst()
                .orElse(InitiatorType.SYSTEM);
    }
}
