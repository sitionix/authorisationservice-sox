package com.sitionix.athssox.application.outbox;

import com.sitionix.athssox.domain.model.RegisterUserDO;
import com.sitionix.athssox.domain.model.ResponseRegisterUser;
import com.sitionix.athssox.domain.model.outbox.OutboxAggregateType;
import com.sitionix.athssox.domain.model.outbox.OutboxEventCreate;
import com.sitionix.athssox.domain.model.outbox.OutboxEventType;
import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRegistrationOutboxCreator {

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    public void create(final RegisterUserDO registerUserDO,
                       final ResponseRegisterUser createdUser) {
        final OutboxEventCreate outboxEvent = OutboxEventCreate.builder()
                .aggregateType(OutboxAggregateType.USER)
                .aggregateId(this.toAggregateId(createdUser.getUserId()))
                .eventType(OutboxEventType.EMAIL_VERIFY)
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .nextRetryAt(LocalDateTime.now(this.clock))
                .payload(this.buildEmailVerifyPayload(registerUserDO, createdUser))
                .build();

        this.outboxEventRepository.create(outboxEvent);
    }

    private UUID toAggregateId(final Long userId) {
        return UUID.nameUUIDFromBytes(("user:" + userId).getBytes(StandardCharsets.UTF_8));
    }

    private String buildEmailVerifyPayload(final RegisterUserDO registerUserDO,
                                           final ResponseRegisterUser createdUser) {
        final String email = this.escapeJson(registerUserDO.getEmail());
        final String userId = this.escapeJson(String.valueOf(createdUser.getUserId()));
        final String siteId = registerUserDO.getSiteId() == null
                ? null
                : this.escapeJson(registerUserDO.getSiteId().toString());

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
