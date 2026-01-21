package com.sitionix.athssox.postgresql.mapper.outbox;

import com.sitionix.athssox.domain.model.outbox.payload.InitiatorType;
import com.sitionix.athssox.postgresql.entity.outbox.OutboxInitiatorTypeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OutboxInitiatorTypeInfraMapperTest {

    private OutboxInitiatorTypeInfraMapper mapper;

    @BeforeEach
    void setUp() {
        this.mapper = new OutboxInitiatorTypeInfraMapper() {
        };
    }

    @Test
    void given_null_initiator_type_when_as_initiator_type_then_return_null() {
        //given
        final OutboxInitiatorTypeEntity given = null;

        //when
        final InitiatorType actual = this.mapper.asInitiatorType(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_initiator_type_entity_when_as_initiator_type_then_return_initiator_type() {
        //given
        final OutboxInitiatorTypeEntity given = this.getOutboxInitiatorTypeEntity(1L, "USER");
        final InitiatorType expected = InitiatorType.USER;

        //when
        final InitiatorType actual = this.mapper.asInitiatorType(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private OutboxInitiatorTypeEntity getOutboxInitiatorTypeEntity(final Long id,
                                                                   final String description) {
        return OutboxInitiatorTypeEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
