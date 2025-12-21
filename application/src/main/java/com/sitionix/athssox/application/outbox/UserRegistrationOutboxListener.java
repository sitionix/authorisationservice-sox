package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.application.event.UserRegisteredEvent;
import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEventCreate;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRegistrationOutboxListener {

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(final UserRegisteredEvent event) {
        final OutboxEventCreate outboxEvent = OutboxEventCreate.builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(this.toAggregateId(event.getUserId()))
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now(this.clock))
                .payload(this.buildEmailVerifyPayload(event))
                .build();

        this.outboxEventRepository.create(outboxEvent);
    }

    private UUID toAggregateId(final Long userId) {
        return UUID.nameUUIDFromBytes(("user:" + userId).getBytes(StandardCharsets.UTF_8));
    }

    private String buildEmailVerifyPayload(final UserRegisteredEvent event) {
        final String email = this.escapeJson(event.getEmail());
        final String userId = this.escapeJson(String.valueOf(event.getUserId()));
        final String siteId = event.getSiteId() == null ? null : this.escapeJson(event.getSiteId().toString());

        final String siteIdValue = siteId == null ? "null" : "\"" + siteId + "\"";

        return "{"
                + "\"delivery\":{\"channel\":\"EMAIL\",\"to\":\"" + email + "\"},"
                + "\"template\":\"EMAIL_VERIFY\","
                + "\"params\":{},"
                + "\"meta\":{\"userId\":\"" + userId + "\",\"siteId\":" + siteIdValue + "}"
                + "}";
    }

    private String escapeJson(final String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
