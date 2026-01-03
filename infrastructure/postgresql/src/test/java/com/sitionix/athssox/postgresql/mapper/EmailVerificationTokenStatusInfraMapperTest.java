package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.postgresql.entity.EmailVerificationTokenStatusEntity;
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
    void givenNullStatusEntity_whenAsStatus_thenReturnNull() {
        //given
        final EmailVerificationTokenStatusEntity given = null;

        //when
        final EmailVerificationTokenStatus actual = this.emailVerificationTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenStatusEntity_whenAsStatus_thenReturnStatus() {
        //given
        final EmailVerificationTokenStatusEntity given = this.getStatusEntity(1L, "ACTIVE");
        final EmailVerificationTokenStatus expected = EmailVerificationTokenStatus.ACTIVE;

        //when
        final EmailVerificationTokenStatus actual = this.emailVerificationTokenStatusInfraMapper.asStatus(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void givenNullStatus_whenAsStatusEntity_thenReturnNull() {
        //given
        final EmailVerificationTokenStatus given = null;

        //when
        final EmailVerificationTokenStatusEntity actual = this.emailVerificationTokenStatusInfraMapper.asStatusEntity(given);

        //then
        assertThat(actual).isNull();
    }

    @Test
    void givenStatus_whenAsStatusEntity_thenReturnEntity() {
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
