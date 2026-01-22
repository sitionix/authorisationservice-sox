package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.exception.EmailVerificationTokenExpiredException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenInvalidException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenNotFoundException;
import com.sitionix.athssox.domain.exception.UserAlreadyVerifiedException;
import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationLinkIssue;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationTokenSigner;
import com.sitionix.athssox.domain.service.TokenHasher;
import com.sitionix.athssox.domain.service.VerificationLinkFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueEmailVerificationLinkImplTest {

    private IssueEmailVerificationLinkImpl issueEmailVerificationLink;

    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private EmailVerificationTokenSigner tokenSigner;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private VerificationLinkFactory verificationLinkFactory;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.issueEmailVerificationLink = new IssueEmailVerificationLinkImpl(this.emailVerificationTokenRepository,
                this.authUserRepository,
                this.tokenSigner,
                this.tokenHasher,
                this.verificationLinkFactory,
                this.clock);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.emailVerificationTokenRepository,
                this.authUserRepository,
                this.tokenSigner,
                this.tokenHasher,
                this.verificationLinkFactory,
                this.clock);
    }

    @Test
    void given_active_token_and_pending_user_when_issue_link_then_return_issue() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final Instant expiresAt = this.getExpiresAt();
        final String token = this.getToken();
        final String tokenHash = this.getTokenHash();
        final String verifyUrl = this.getVerifyUrl();

        final EmailVerificationTokenRecord tokenRecord = this.getTokenRecord(tokenId,
                this.getUserId(),
                this.getSiteId(),
                tokenHash,
                EmailVerificationTokenStatus.ACTIVE,
                expiresAt,
                null);
        final AuthUser user = this.getUser(UserStatus.PENDING_EMAIL_VERIFY);

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(now);
        when(this.authUserRepository.findById(this.getUserId()))
                .thenReturn(Optional.of(user));
        when(this.tokenSigner.buildToken(tokenId, pepperId))
                .thenReturn(token);
        when(this.tokenHasher.hash(token))
                .thenReturn(tokenHash);
        when(this.verificationLinkFactory.buildEmailVerifyUrl(token, this.getSiteId()))
                .thenReturn(verifyUrl);

        //when
        final EmailVerificationLinkIssue actual = this.issueEmailVerificationLink.execute(tokenId, pepperId);

        //then
        final EmailVerificationLinkIssue expected = this.getEmailVerificationLinkIssue(tokenId, verifyUrl, expiresAt);
        assertThat(actual).isEqualTo(expected);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(this.getUserId());
        verify(this.tokenSigner)
                .buildToken(tokenId, pepperId);
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationLinkFactory)
                .buildEmailVerifyUrl(token, this.getSiteId());
    }

    @Test
    void given_missing_token_when_issue_link_then_throw_not_found() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.empty());

        //when
        final Throwable actual = catchThrowable(() -> this.issueEmailVerificationLink.execute(tokenId, pepperId));

        //then
        assertThat(actual).isInstanceOf(EmailVerificationTokenNotFoundException.class);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
    }

    @Test
    void given_expired_token_when_issue_link_then_throw_expired() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final EmailVerificationTokenRecord tokenRecord = this.getTokenRecord(tokenId,
                this.getUserId(),
                this.getSiteId(),
                this.getTokenHash(),
                EmailVerificationTokenStatus.ACTIVE,
                this.getExpiredAt(),
                null);

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(now);

        //when
        final Throwable actual = catchThrowable(() -> this.issueEmailVerificationLink.execute(tokenId, pepperId));

        //then
        assertThat(actual).isInstanceOf(EmailVerificationTokenExpiredException.class);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
        verify(this.clock)
                .instant();
    }

    @Test
    void given_used_token_when_issue_link_then_throw_invalid() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final EmailVerificationTokenRecord tokenRecord = this.getTokenRecord(tokenId,
                this.getUserId(),
                this.getSiteId(),
                this.getTokenHash(),
                EmailVerificationTokenStatus.USED,
                this.getExpiresAt(),
                this.getUsedAt());

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(now);

        //when
        final Throwable actual = catchThrowable(() -> this.issueEmailVerificationLink.execute(tokenId, pepperId));

        //then
        assertThat(actual).isInstanceOf(EmailVerificationTokenInvalidException.class);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
        verify(this.clock)
                .instant();
    }

    @Test
    void given_active_user_when_issue_link_then_throw_conflict() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final EmailVerificationTokenRecord tokenRecord = this.getTokenRecord(tokenId,
                this.getUserId(),
                this.getSiteId(),
                this.getTokenHash(),
                EmailVerificationTokenStatus.ACTIVE,
                this.getExpiresAt(),
                null);
        final AuthUser user = this.getUser(UserStatus.ACTIVE);

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(now);
        when(this.authUserRepository.findById(this.getUserId()))
                .thenReturn(Optional.of(user));

        //when
        final Throwable actual = catchThrowable(() -> this.issueEmailVerificationLink.execute(tokenId, pepperId));

        //then
        assertThat(actual).isInstanceOf(UserAlreadyVerifiedException.class);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(this.getUserId());
    }

    @Test
    void given_hash_mismatch_when_issue_link_then_throw_invalid() {
        //given
        final UUID tokenId = this.getTokenId();
        final UUID pepperId = this.getPepperId();
        final Instant now = this.getNow();
        final EmailVerificationTokenRecord tokenRecord = this.getTokenRecord(tokenId,
                this.getUserId(),
                this.getSiteId(),
                this.getTokenHash(),
                EmailVerificationTokenStatus.ACTIVE,
                this.getExpiresAt(),
                null);
        final AuthUser user = this.getUser(UserStatus.PENDING_EMAIL_VERIFY);

        when(this.emailVerificationTokenRepository.findById(tokenId))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(now);
        when(this.authUserRepository.findById(this.getUserId()))
                .thenReturn(Optional.of(user));
        when(this.tokenSigner.buildToken(tokenId, pepperId))
                .thenReturn(this.getToken());
        when(this.tokenHasher.hash(this.getToken()))
                .thenReturn("mismatch");

        //when
        final Throwable actual = catchThrowable(() -> this.issueEmailVerificationLink.execute(tokenId, pepperId));

        //then
        assertThat(actual).isInstanceOf(EmailVerificationTokenInvalidException.class);
        verify(this.emailVerificationTokenRepository)
                .findById(tokenId);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(this.getUserId());
        verify(this.tokenSigner)
                .buildToken(tokenId, pepperId);
        verify(this.tokenHasher)
                .hash(this.getToken());
    }

    private Throwable catchThrowable(final Runnable action) {
        try {
            action.run();
            return null;
        } catch (final Throwable throwable) {
            return throwable;
        }
    }

    private UUID getTokenId() {
        return UUID.fromString("8f24d9f6-2c05-4b77-8c4e-1bc6e1ba9b6c");
    }

    private UUID getPepperId() {
        return UUID.fromString("d5d2d5de-6930-43c0-9e45-9a8e6dbe8292");
    }

    private Long getUserId() {
        return 14L;
    }

    private UUID getSiteId() {
        return UUID.fromString("c9b1f3f4-12c7-11ec-82a8-0242ac130003");
    }

    private Instant getNow() {
        return Instant.parse("2024-05-01T10:15:30Z");
    }

    private Instant getExpiresAt() {
        return Instant.parse("2024-06-01T10:15:30Z");
    }

    private Instant getExpiredAt() {
        return Instant.parse("2024-04-01T10:15:30Z");
    }

    private Instant getUsedAt() {
        return Instant.parse("2024-05-02T10:15:30Z");
    }

    private String getToken() {
        return "token-id.pepper-id.signature";
    }

    private String getTokenHash() {
        return "token-hash";
    }

    private String getVerifyUrl() {
        return "https://bff.example.com/api/v1/auth/email/verify?token=token";
    }

    private EmailVerificationTokenRecord getTokenRecord(final UUID tokenId,
                                                        final Long userId,
                                                        final UUID siteId,
                                                        final String tokenHash,
                                                        final EmailVerificationTokenStatus status,
                                                        final Instant expiresAt,
                                                        final Instant usedAt) {
        return EmailVerificationTokenRecord.builder()
                .id(tokenId)
                .userId(userId)
                .siteId(siteId)
                .tokenHash(tokenHash)
                .status(status)
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
    }

    private AuthUser getUser(final UserStatus status) {
        return AuthUser.builder()
                .id(this.getUserId())
                .status(status)
                .build();
    }

    private EmailVerificationLinkIssue getEmailVerificationLinkIssue(final UUID tokenId,
                                                                     final String verifyUrl,
                                                                     final Instant expiresAt) {
        return new EmailVerificationLinkIssue(tokenId, verifyUrl, expiresAt);
    }
}
