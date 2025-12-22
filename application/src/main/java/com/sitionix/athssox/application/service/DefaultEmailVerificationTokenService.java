package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.EmailVerificationTokenService;
import com.sitionix.athssox.domain.service.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultEmailVerificationTokenService implements EmailVerificationTokenService {

    private final SecureRandom secureRandom;
    private final TokenHasher tokenHasher;

    @Override
    public String issue(final UUID userId, final UUID siteId) {
        final String rawToken = generateToken();
        final String tokenHash = tokenHasher.hash(rawToken);

        //TODO: persist token when verify email flow be ready with tabele and repository

        return rawToken;
    }


    private String generateToken() {
        final byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
