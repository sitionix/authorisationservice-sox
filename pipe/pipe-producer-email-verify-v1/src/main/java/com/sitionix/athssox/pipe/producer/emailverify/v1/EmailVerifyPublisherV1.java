package com.sitionix.athssox.pipe.producer.emailverify.v1;

import com.app_afesox.athssox.events.emailverify.EmailVerifyEvent;
import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.athssox.events.emailverify.kafka.EmailverifyV1Producer;
import com.app_afesox.events.Metadata;
import com.sitionix.athssox.domain.service.EmailVerifyPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyPublisherV1 implements EmailVerifyPublisher {

    private static final String EVENT_TYPE = "EmailVerifyEvent";

    private final EmailverifyV1Producer producer;


    @Override
    public void publish() {
        final String eventId = UUID.randomUUID().toString();
        final EmailVerifyEvent payload = EmailVerifyEvent.newBuilder()
                .build();
        final Metadata metadata = Metadata.newBuilder()
                .setIdempotencyId(eventId)
                .setCreatedAt(Instant.now().toEpochMilli())
                .setEventType(EVENT_TYPE)
                .build();
        final EmailVerifyEventEnvelope envelope = EmailVerifyEventEnvelope.newBuilder()
                .setMetadata(metadata)
                .setPayload(payload)
                .build();

        this.producer.send(eventId, envelope);
        log.info("Email verify event published with eventId={}", eventId);
    }
}
