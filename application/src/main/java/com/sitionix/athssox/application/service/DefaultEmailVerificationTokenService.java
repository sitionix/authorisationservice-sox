package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.TokenConfig;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenRecord;
import com.sitionix.athssox.domain.model.emailverify.EmailVerificationTokenStatus;
import com.sitionix.athssox.domain.repository.EmailVerificationTokenRepository;
import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import com.sitionix.athssox.domain.service.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultEmailVerificationTokenService implements EmailVerificationTokenService {

    private final SecureRandom secureRandom;
    private final TokenHasher tokenHasher;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final TokenConfig tokenConfig;
    private final Clock clock;

    @Override
    public String issue(final Long userId, final UUID siteId) {
        final String rawToken = generateToken();
        final String tokenHash = tokenHasher.hash(rawToken);

        final Instant expiresAt = this.clock.instant()
                .plusSeconds(this.tokenConfig.getEmailVerificationTokenTtlSeconds());

        final EmailVerificationTokenRecord tokenRecord = EmailVerificationTokenRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .siteId(siteId)
                .tokenHash(tokenHash)
                .status(EmailVerificationTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .usedAt(null)
                .build();

        this.emailVerificationTokenRepository.save(tokenRecord);

        return rawToken;
    }


    private String generateToken() {
        final byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
