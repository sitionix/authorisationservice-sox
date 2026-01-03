package com.sitionix.athssox.application.usecase;

import com.sitionix.athssox.domain.model.AuthUser;
import com.sitionix.athssox.domain.model.UserRole;
import com.sitionix.athssox.domain.model.UserStatus;
import com.sitionix.athssox.domain.model.emailverify.EmailVerification;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.AuthUserRepository;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerifyEmailImplTest {

    private static final Instant NOW = Instant.parse("2024-05-01T10:15:30Z");

    private VerifyEmailImpl verifyEmail;

    private UUID recordSiteId;
    private EmailVerificationTokenStatus recordStatus;
    private Instant recordExpiresAt;
    private Instant recordUsedAt;

    @Mock
    private TokenHasher tokenHasher;

    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private Clock clock;

    @BeforeEach
    void setUp() {
        this.verifyEmail = new VerifyEmailImpl(this.tokenHasher,
                this.verificationTokenRepository,
                this.authUserRepository,
                this.clock);
        this.recordSiteId = null;
        this.recordStatus = null;
        this.recordExpiresAt = null;
        this.recordUsedAt = null;
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(this.tokenHasher,
                this.verificationTokenRepository,
                this.authUserRepository,
                this.clock);
    }

    @Test
    void givenMissingTokenRecord_whenExecute_thenReturnFalse() {
        //given
        final UUID siteId = this.getSiteId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.empty());

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
    }

    @Test
    void givenExpiredToken_whenExecute_thenReturnFalse() {
        //given
        final UUID siteId = this.getSiteId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);
        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        this.recordExpiresAt = this.getExpiredAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
    }

    @Test
    void givenNonActiveToken_whenExecute_thenReturnFalse() {
        //given
        final UUID siteId = this.getSiteId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);
        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.USED;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
    }

    @Test
    void givenSiteMismatch_whenExecute_thenReturnFalse() {
        //given
        final UUID tokenSiteId = this.getSiteId();
        final UUID requestSiteId = this.getOtherSiteId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(requestSiteId, token);
        this.recordSiteId = tokenSiteId;
        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
    }

    @Test
    void givenMissingUser_whenExecute_thenReturnFalse() {
        //given
        final UUID siteId = this.getSiteId();
        final Long userId = this.getUserId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);
        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.empty());

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(userId);
    }

    @Test
    void givenUserNotPending_whenExecute_thenReturnFalse() {
        //given
        final UUID siteId = this.getSiteId();
        final Long userId = this.getUserId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);
        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();
        final AuthUser authUser = this.getAuthUser(userId, UserStatus.ACTIVE, siteId);

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.of(authUser));

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        assertThat(actual).isFalse();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(userId);
    }

    @Test
    void givenPendingUser_whenExecute_thenActivateUserAndMarkTokenUsed() {
        //given
        final UUID siteId = this.getSiteId();
        final Long userId = this.getUserId();
        final String token = this.getToken();
        final String hashedToken = this.getHashedToken();
        final EmailVerification given = this.getEmailVerification(siteId, token);
        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.ACTIVE;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = null;

        final EmailVerificationTokenRecord tokenRecord = this.getEmailVerificationTokenRecord();
        final AuthUser authUser = this.getAuthUser(userId, UserStatus.PENDING_EMAIL_VERIFY, siteId);

        when(this.tokenHasher.hash(token))
                .thenReturn(hashedToken);
        when(this.verificationTokenRepository.findByHashedToken(hashedToken))
                .thenReturn(Optional.of(tokenRecord));
        when(this.clock.instant())
                .thenReturn(NOW);
        when(this.authUserRepository.findById(userId))
                .thenReturn(Optional.of(authUser));

        //when
        final boolean actual = this.verifyEmail.execute(given);

        //then
        final ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        final ArgumentCaptor<EmailVerificationTokenRecord> recordCaptor =
                ArgumentCaptor.forClass(EmailVerificationTokenRecord.class);

        assertThat(actual).isTrue();
        verify(this.tokenHasher)
                .hash(token);
        verify(this.verificationTokenRepository)
                .findByHashedToken(hashedToken);
        verify(this.clock)
                .instant();
        verify(this.authUserRepository)
                .findById(userId);
        verify(this.authUserRepository)
                .save(userCaptor.capture());
        verify(this.verificationTokenRepository)
                .save(recordCaptor.capture());

        final AuthUser expectedUser = this.getAuthUser(userId, UserStatus.ACTIVE, siteId);
        assertThat(userCaptor.getValue()).isEqualTo(expectedUser);

        this.recordSiteId = siteId;
        this.recordStatus = EmailVerificationTokenStatus.USED;
        this.recordExpiresAt = this.getValidExpiresAt();
        this.recordUsedAt = NOW;

        final EmailVerificationTokenRecord expectedRecord = this.getEmailVerificationTokenRecord();
        assertThat(recordCaptor.getValue()).isEqualTo(expectedRecord);
    }

    private UUID getSiteId() {
        return UUID.fromString("5a54b1d4-0e41-4d71-a5ee-46881f0fdc82");
    }

    private UUID getOtherSiteId() {
        return UUID.fromString("4db0d3a1-208a-4dfc-b2f8-4ae2e0f0c6c0");
    }

    private UUID getTokenId() {
        return UUID.fromString("0ef9fdea-83ee-4ef5-910f-40b2f6f60265");
    }

    private Long getUserId() {
        return 31L;
    }

    private String getToken() {
        return "raw-token";
    }

    private String getHashedToken() {
        return "hashed-token";
    }

    private Instant getExpiredAt() {
        return NOW.minusSeconds(60);
    }

    private Instant getValidExpiresAt() {
        return NOW.plusSeconds(3600);
    }

    private EmailVerification getEmailVerification(final UUID siteId, final String token) {
        return EmailVerification.builder()
                .siteId(siteId)
                .token(token)
                .build();
    }

    private EmailVerificationTokenRecord getEmailVerificationTokenRecord() {
        return EmailVerificationTokenRecord.builder()
                .id(this.getTokenId())
                .userId(this.getUserId())
                .siteId(this.getRecordSiteId())
                .tokenHash(this.getHashedToken())
                .status(this.getRecordStatus())
                .expiresAt(this.getRecordExpiresAt())
                .usedAt(this.getRecordUsedAt())
                .build();
    }

    private UUID getRecordSiteId() {
        return this.recordSiteId;
    }

    private EmailVerificationTokenStatus getRecordStatus() {
        return this.recordStatus;
    }

    private Instant getRecordExpiresAt() {
        return this.recordExpiresAt;
    }

    private Instant getRecordUsedAt() {
        return this.recordUsedAt;
    }

    private AuthUser getAuthUser(final Long userId, final UserStatus status, final UUID siteId) {
        return AuthUser.builder()
                .id(userId)
                .email("user@sitionix.com")
                .passwordHash("hashed")
                .status(status)
                .role(UserRole.SITE_USER)
                .siteId(siteId)
                .build();
    }
}
