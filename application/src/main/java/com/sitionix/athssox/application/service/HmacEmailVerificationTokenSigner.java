package com.sitionix.athssox.application.service;

import com.sitionix.athssox.application.config.EmailVerificationSecurityConfig;
import com.sitionix.athssox.domain.service.EmailVerificationTokenSigner;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HmacEmailVerificationTokenSigner implements EmailVerificationTokenSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final EmailVerificationSecurityConfig securityConfig;

    @PostConstruct
    void validateSecret() {
        if (!StringUtils.hasText(this.securityConfig.getHmacSecret())) {
            throw new IllegalStateException("Email verification HMAC secret must be configured.");
        }
    }

    @Override
    public String sign(final UUID tokenId, final UUID pepperId) {
        final String data = tokenId + "." + pepperId;
        final byte[] signature = this.hmacSha256(data);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature);
    }

    @Override
    public String buildToken(final UUID tokenId, final UUID pepperId) {
        return tokenId + "." + pepperId + "." + this.sign(tokenId, pepperId);
    }

    private byte[] hmacSha256(final String data) {
        try {
            final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(this.securityConfig.getHmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Failed to sign email verification token", exception);
        }
    }
}
