package com.sitionix.athssox.it.infra;

import com.app_afesox.athssox.events.emailverify.EmailVerifyEvent;
import com.app_afesox.athssox.events.emailverify.EmailVerifyEventEnvelope;
import com.app_afesox.athssox.events.kafka.AvroRecordDeserializer;
import com.app_afesox.events.Metadata;
import com.sitionix.forgeit.kafka.api.KafkaContract;

public class OutboxKafkaContracts {

    public static final KafkaContract<EmailVerifyEventEnvelope> EMAIL_VERIFY_EVENT_ENVELOPE_KAFKA_CONTRACT =
            KafkaContract.consumerContract()
                    .topicFromProperty("forge-it.kafka.email-verify-topic")
                    .groupId("forge-it-group")
                    .payloadDeserializer(AvroRecordDeserializer.class)
                    .defaultEnvelope(EmailVerifyEventEnvelope.class)
                    .defaultExpectedPayload(EmailVerifyEvent.class, "defaultUserCreatedEvent.json")
                    .defaultMetadata(Metadata.class, "defaultUserCreatedMetadata.json")
                    .build();
}
