package com.sitionix.athssox.pipe.producer;

import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.athssox.events.emailverify.kafka.EmailverifyV1Producer;
import com.sitionix.athssox.domain.event.EventHandler;
import com.sitionix.athssox.domain.model.outbox.payload.EmailVerifyPayload;
import com.sitionix.athssox.domain.model.outbox.payload.Event;
import com.sitionix.athssox.pipe.producer.mapper.EmailVerifyEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyPublisherV1 implements EventHandler<EmailVerifyPayload> {

    private final EmailverifyV1Producer producer;

    private final EmailVerifyEventMapper mapper;

    @Override
    public void publish(final Event<EmailVerifyPayload> event) {
        log.info("Publish email verify event: {}", event);
        if (isNull(event)) {
            return;
        }

        final EmailVerifyEventEnvelope envelope = this.mapper.asEnvelope(event);

        this.producer.send(event.getId(), envelope);
        log.info("Event email verify published: {}", envelope);
    }

}
