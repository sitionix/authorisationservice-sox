package com.sitionix.athssox.application.service;

import com.sitionix.athssox.domain.service.VerificationLinkFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class DefaultVerificationLinkFactory implements VerificationLinkFactory {

    @Value("${bff:base-url}")
    private String baseUrl;

    @Override
    public String buildEmailVerifyUrl(String rawToken, UUID siteId) {
        final String token = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        final String sid = URLEncoder.encode(siteId.toString(), StandardCharsets.UTF_8);

        return baseUrl + "/api/v1/auth/email/verify?token=" + token + "&siteId=" + sid;    }
}
