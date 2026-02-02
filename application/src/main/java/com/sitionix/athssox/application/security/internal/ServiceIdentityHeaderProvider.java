package com.sitionix.athssox.application.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServiceIdentityHeaderProvider {

    private final InternalAuthConfig internalAuthConfig;
    private final ServiceJwtIssuer serviceJwtIssuer;

    public Optional<String> getAuthorizationHeader(final String audience, final List<String> scopes) {
        if (this.internalAuthConfig.getMode() != InternalAuthMode.DEV_JWT) {
            return Optional.empty();
        }
        final String token = this.serviceJwtIssuer.issueToken(audience, scopes);
        return Optional.of("Bearer " + token);
    }
}
