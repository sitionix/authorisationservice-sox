package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.OutboxStatus;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxStatusInfraMapperTest {

    private OutboxStatusInfraMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxStatusInfraMapper() {
        };
    }

    @Test
    void given_null_status_when_as_outbox_status_entity_then_return_null() {
        //given
        final OutboxStatus given = null;

        //when
        final OutboxStatusEntity actual = this.mapper.asOutboxStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_status_when_as_outbox_status_entity_then_return_entity() {
        //given
        final OutboxStatus given = OutboxStatus.PENDING;
        final OutboxStatusEntity expected = this.getOutboxStatusEntity(1L, "PENDING");

        //when
        final OutboxStatusEntity actual = this.mapper.asOutboxStatusEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_status_entity_when_as_event_type_then_return_status() {
        //given
        final OutboxStatusEntity given = this.getOutboxStatusEntity(1L, "PENDING");
        final OutboxStatus expected = OutboxStatus.PENDING;

        //when
        final OutboxStatus actual = this.mapper.asEventType(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private OutboxStatusEntity getOutboxStatusEntity(final Long id, final String description) {
        return OutboxStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
