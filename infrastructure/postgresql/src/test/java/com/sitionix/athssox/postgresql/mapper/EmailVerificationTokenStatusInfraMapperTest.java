package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenStatusEntity;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenStatusInfraMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenStatusInfraMapperTest {

    private EmailVerificationTokenStatusInfraMapper emailVerificationTokenStatusInfraMapper;

    @BeforeEach
    void setUp() {
        this.emailVerificationTokenStatusInfraMapper = new EmailVerificationTokenStatusInfraMapper() {
        };
    }

    @Test
    void given_null_status_entity_when_as_status_then_return_null() {
        //given
        final EmailVerificationTokenStatusEntity given = null;

        //when
        final EmailVerificationTokenStatus actual = this.emailVerificationTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_status_entity_when_as_status_then_return_status() {
        //given
        final EmailVerificationTokenStatusEntity given = this.getStatusEntity(1L, "ACTIVE");
        final EmailVerificationTokenStatus expected = EmailVerificationTokenStatus.ACTIVE;

        //when
        final EmailVerificationTokenStatus actual = this.emailVerificationTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_null_status_when_as_status_entity_then_return_null() {
        //given
        final EmailVerificationTokenStatus given = null;

        //when
        final EmailVerificationTokenStatusEntity actual = this.emailVerificationTokenStatusInfraMapper.asStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void given_status_when_as_status_entity_then_return_entity() {
        //given
        final EmailVerificationTokenStatus given = EmailVerificationTokenStatus.ACTIVE;
        final EmailVerificationTokenStatusEntity expected = this.getStatusEntity(1L, "ACTIVE");

        //when
        final EmailVerificationTokenStatusEntity actual = this.emailVerificationTokenStatusInfraMapper.asStatusEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private EmailVerificationTokenStatusEntity getStatusEntity(final Long id, final String description) {
        return EmailVerificationTokenStatusEntity.builder()
                .id(id)
                .description(description)
                .build();
    }
}
