package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.TokenHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultEmailVerificationTokenServiceTest {

    private DefaultEmailVerificationTokenService defaultEmailVerificationTokenService;

    private FixedSecureRandom secureRandom;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private TokenConfig tokenConfig;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.secureRandom = this.getSecureRandom();
        this.defaultEmailVerificationTokenService = new DefaultEmailVerificationTokenService(this.secureRandom,
                this.tokenHasher,
                this.emailVerificationTokenRepository,
                this.tokenConfig,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenHasher,
                this.emailVerificationTokenRepository,
                this.tokenConfig,
                this.clock);
    }

    @Test
    void givenUserIdAndSiteId_whenIssue_thenPersistRecordAndReturnRawToken() {
        //given
        final Long userId = this.getUserId();
        final UUID siteId = this.getSiteId();
        final Instant now = this.getNow();
        final long ttlSeconds = this.getTtlSeconds();
        final byte[] tokenBytes = this.getTokenBytes();
        final String rawToken = this.getRawToken(tokenBytes);
        final String hashedToken = this.getHashedToken();

        this.secureRandom.setNextBytes(tokenBytes);
        when(this.tokenHasher.hash(rawToken))
                .thenReturn(hashedToken);
        when(this.tokenConfig.getEmailVerificationTokenTtlSeconds())
                .thenReturn(ttlSeconds);
        when(this.clock.instant())
                .thenReturn(now);

        //when
        final String actual = this.defaultEmailVerificationTokenService.issue(userId, siteId);

        //then
        final ArgumentCaptor<EmailVerificationTokenRecord> recordCaptor =
                ArgumentCaptor.forClass(EmailVerificationTokenRecord.class);

        assertThat(actual).isEqualTo(rawToken);
        assertThat(this.secureRandom.getCallCount()).isEqualTo(1);
        verify(this.tokenHasher)
                .hash(rawToken);
        verify(this.tokenConfig)
                .getEmailVerificationTokenTtlSeconds();
        verify(this.clock)
                .instant();
        verify(this.emailVerificationTokenRepository)
                .save(recordCaptor.capture());

        final EmailVerificationTokenRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getId()).isNotNull();

        final EmailVerificationTokenRecord expected = this.getEmailVerificationTokenRecord(savedRecord.getId(),
                userId,
                siteId,
                hashedToken,
                now.plusSeconds(ttlSeconds),
                EmailVerificationTokenStatus.ACTIVE,
                null);

        assertThat(savedRecord).isEqualTo(expected);
    }

    private Long getUserId() {
        return 14L;
    }

    private UUID getSiteId() {
        return UUID.fromString("8f24d9f6-2c05-4b77-8c4e-1bc6e1ba9b6c");
    }

    private Instant getNow() {
        return Instant.parse("2024-05-01T10:15:30Z");
    }

    private long getTtlSeconds() {
        return 3600L;
    }

    private FixedSecureRandom getSecureRandom() {
        return new FixedSecureRandom();
    }

    private byte[] getTokenBytes() {
        final byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        return bytes;
    }

    private String getRawToken(final byte[] bytes) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String getHashedToken() {
        return "hashed-token";
    }

    private EmailVerificationTokenRecord getEmailVerificationTokenRecord(final UUID id,
                                                                         final Long userId,
                                                                         final UUID siteId,
                                                                         final String tokenHash,
                                                                         final Instant expiresAt,
                                                                         final EmailVerificationTokenStatus status,
                                                                         final Instant usedAt) {
        return EmailVerificationTokenRecord.builder()
                .id(id)
                .userId(userId)
                .siteId(siteId)
                .tokenHash(tokenHash)
                .status(status)
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
    }

    private static final class FixedSecureRandom extends SecureRandom {

        private byte[] nextBytes;
        private int callCount;

        private void setNextBytes(final byte[] nextBytes) {
            this.nextBytes = nextBytes.clone();
        }

        private int getCallCount() {
            return this.callCount;
        }

        @Override
        public void nextBytes(final byte[] bytes) {
            this.callCount++;
            System.arraycopy(this.nextBytes, 0, bytes, 0, bytes.length);
        }
    }
}
