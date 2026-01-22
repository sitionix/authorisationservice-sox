package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationTokenSigner;
import com.sitionix.athssox.domain.service.PepperIdGenerator;
import com.sitionix.athssox.domain.service.TokenHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultEmailVerificationTokenServiceTest {

    private DefaultEmailVerificationTokenService defaultEmailVerificationTokenService;

    @Mock
    private PepperIdGenerator pepperIdGenerator;

    @Mock
    private EmailVerificationTokenSigner tokenSigner;

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
        this.defaultEmailVerificationTokenService = new DefaultEmailVerificationTokenService(this.pepperIdGenerator,
                this.tokenSigner,
                this.tokenHasher,
                this.emailVerificationTokenRepository,
                this.tokenConfig,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.pepperIdGenerator,
                this.tokenSigner,
                this.tokenHasher,
                this.emailVerificationTokenRepository,
                this.tokenConfig,
                this.clock);
    }

    @Test
    void given_user_id_and_site_id_when_issue_then_persist_record_and_return_token_issue() {
        //given
        final Long userId = this.getUserId();
        final UUID siteId = this.getSiteId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final long ttlSeconds = this.getTtlSeconds();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();

        when(this.pepperIdGenerator.generate())
                .thenReturn(pepperId);
        when(this.tokenSigner.buildToken(any(UUID.class), eq(pepperId)))
                .thenReturn(token);
        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.tokenConfig.getEmailVerificationTokenTtlSeconds())
                .thenReturn(ttlSeconds);
        when(this.clock.instant())
                .thenReturn(now);

        //when
        final EmailVerificationTokenIssue actual = this.defaultEmailVerificationTokenService.issue(userId, siteId);

        //then
        final ArgumentCaptor<EmailVerificationTokenRecord> recordCaptor =
                ArgumentCaptor.forClass(EmailVerificationTokenRecord.class);
        final ArgumentCaptor<UUID> tokenIdCaptor = ArgumentCaptor.forClass(UUID.class);

        assertThat(actual.tokenId()).isNotNull();
        assertThat(actual.pepperId()).isEqualTo(pepperId);
        verify(this.pepperIdGenerator)
                .generate();
        verify(this.tokenSigner)
                .buildToken(tokenIdCaptor.capture(), eq(pepperId));
        verify(this.tokenHasher)
                .hash(token);
        verify(this.tokenConfig)
                .getEmailVerificationTokenTtlSeconds();
        verify(this.clock)
                .instant();
        verify(this.emailVerificationTokenRepository)
                .save(recordCaptor.capture());

        final EmailVerificationTokenRecord savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getId()).isEqualTo(actual.tokenId());
        assertThat(actual.tokenId()).isEqualTo(tokenIdCaptor.getValue());

        final EmailVerificationTokenRecord expected = this.getEmailVerificationTokenRecord(actual.tokenId(),
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

    private UUID getPepperId() {
        return UUID.fromString("2cf629c1-1b58-4aa3-a9fd-5e9be2b1d31d");
    }

    private Instant getNow() {
        return Instant.parse("2024-05-01T10:15:30Z");
    }

    private long getTtlSeconds() {
        return 3600L;
    }

    private String getToken() {
        return "token-id.pepper-id.signature";
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

}
