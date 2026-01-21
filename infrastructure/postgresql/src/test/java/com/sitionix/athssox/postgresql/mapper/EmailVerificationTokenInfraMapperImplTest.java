package com.sitionix.athssox.postgresql.mapper;

import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenEntity;
import com.sitionix.athssox.postgresql.entity.token.EmailVerificationTokenStatusEntity;
import com.sitionix.athssox.postgresql.entity.user.UserEntity;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenInfraMapper;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenInfraMapperImpl;
import com.sitionix.athssox.postgresql.mapper.token.EmailVerificationTokenStatusInfraMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTokenInfraMapperImplTest {

    private EmailVerificationTokenInfraMapper emailVerificationTokenInfraMapper;
    private EmailVerificationTokenStatus recordStatus;

    @BeforeEach
    void setUp() {
        final EmailVerificationTokenStatusInfraMapper statusMapper = new EmailVerificationTokenStatusInfraMapper() {
        };
        this.emailVerificationTokenInfraMapper = new EmailVerificationTokenInfraMapperImpl(statusMapper);
        this.recordStatus = null;
    }

    @Test
    void given_record_when_as_entity_then_return_entity() {
        //given
        final UUID id = this.getTokenId();
        final Long userId = this.getUserId();
        final UUID siteId = this.getSiteId();
        final Instant expiresAt = this.getExpiresAt();
        final Instant usedAt = this.getUsedAt();

        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        final EmailVerificationTokenRecord given = this.getEmailVerificationTokenRecord();
        final EmailVerificationTokenEntity expected = this.getEmailVerificationTokenEntity(id,
                userId,
                siteId,
                EmailVerificationTokenStatus.ACTIVE,
                this.getTokenHash(),
                expiresAt,
                usedAt);

        //when
        final EmailVerificationTokenEntity actual = this.emailVerificationTokenInfraMapper.asEntity(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void given_entity_when_as_record_then_return_record() {
        //given
        final UUID id = this.getTokenId();
        final Long userId = this.getUserId();
        final UUID siteId = this.getSiteId();
        final Instant expiresAt = this.getExpiresAt();
        final Instant usedAt = this.getUsedAt();

        final EmailVerificationTokenEntity given = this.getEmailVerificationTokenEntity(id,
                userId,
                siteId,
                EmailVerificationTokenStatus.USED,
                this.getTokenHash(),
                expiresAt,
                usedAt);
        this.recordStatus = EmailVerificationTokenStatus.USED;
        final EmailVerificationTokenRecord expected = this.getEmailVerificationTokenRecord();

        //when
        final EmailVerificationTokenRecord actual = this.emailVerificationTokenInfraMapper.asRecord(given);

        //then
        assertThat(actual).isEqualTo(expected);
    }

    private UUID getTokenId() {
        return UUID.fromString("f14fae8f-1f2c-4ef3-9f1c-c11b3aa64c52");
    }

    private Long getUserId() {
        return 12L;
    }

    private UUID getSiteId() {
        return UUID.fromString("3aa1b12f-1479-4cbe-8c40-469fa5f1c90a");
    }

    private Instant getExpiresAt() {
        return Instant.parse("2024-05-02T10:15:30Z");
    }

    private Instant getUsedAt() {
        return Instant.parse("2024-05-02T11:15:30Z");
    }

    private EmailVerificationTokenRecord getEmailVerificationTokenRecord() {
        return EmailVerificationTokenRecord.builder()
                .id(this.getTokenId())
                .userId(this.getUserId())
                .siteId(this.getSiteId())
                .tokenHash(this.getTokenHash())
                .status(this.getRecordStatus())
                .expiresAt(this.getExpiresAt())
                .usedAt(this.getUsedAt())
                .build();
    }

    private String getTokenHash() {
        return "hashed-token";
    }

    private EmailVerificationTokenStatus getRecordStatus() {
        return this.recordStatus;
    }

    private EmailVerificationTokenEntity getEmailVerificationTokenEntity(final UUID id,
                                                                         final Long userId,
                                                                         final UUID siteId,
                                                                         final EmailVerificationTokenStatus status,
                                                                         final String tokenHash,
                                                                         final Instant expiresAt,
                                                                         final Instant usedAt) {
        return EmailVerificationTokenEntity.builder()
                .id(id)
                .tokenHash(tokenHash)
                .user(this.getUserEntity(userId))
                .siteId(siteId)
                .status(this.getStatusEntity(status))
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
    }

    private UserEntity getUserEntity(final Long userId) {
        return UserEntity.builder()
                .id(userId)
                .build();
    }

    private EmailVerificationTokenStatusEntity getStatusEntity(final EmailVerificationTokenStatus status) {
        return EmailVerificationTokenStatusEntity.builder()
                .id(status.getId())
                .description(status.getDescription())
                .build();
    }
}
