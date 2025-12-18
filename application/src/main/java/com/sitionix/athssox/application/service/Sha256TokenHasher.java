package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.TokenHasher;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class Sha256TokenHasher implements TokenHasher {

    @Override
    public String hash(final String token) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (final byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm not available", exception);
        }
    }
}
