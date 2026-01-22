package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenIssue;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import com.sitionix.athssox.domain.service.EmailVerificationTokenSigner;
import com.sitionix.athssox.domain.service.PepperIdGenerator;
import com.sitionix.athssox.domain.service.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultEmailVerificationTokenService implements EmailVerificationTokenService {

    private final PepperIdGenerator pepperIdGenerator;
    private final EmailVerificationTokenSigner tokenSigner;
    private final TokenHasher tokenHasher;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TokenConfig tokenConfig;
    private final Clock clock;

    @Override
    public EmailVerificationTokenIssue issue(final Long userId, final UUID siteId) {
        final UUID tokenId = UUID.randomUUID();
        final UUID pepperId = this.pepperIdGenerator.generate();
        final String token = this.tokenSigner.buildToken(tokenId, pepperId);
        final String tokenHash = this.tokenHasher.hash(token);

        final Instant expiresAt = this.clock.instant()
                .plusSeconds(this.tokenConfig.getEmailVerificationTokenTtlSeconds());

        final EmailVerificationTokenRecord tokenRecord = EmailVerificationTokenRecord.builder()
                .id(tokenId)
                .userId(userId)
                .siteId(siteId)
                .tokenHash(tokenHash)
                .status(EmailVerificationTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .usedAt(null)
                .build();

        this.emailVerificationTokenRepository.save(tokenRecord);

        return new EmailVerificationTokenIssue(tokenId, pepperId);
    }
}
