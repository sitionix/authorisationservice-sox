package com.sitionix.athssox.security.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class InternalEndpointRequestMatcher implements RequestMatcher {

    private static final String JWKS_PATH = "/.well-known/jwks.json";
    private static final String JWKS_ALIAS_PATH = "/oauth2/v1/keys";

    @Override
    public boolean matches(final HttpServletRequest request) {
        final String path = InternalAuthRequestHelper.resolvePath(request);
        return !JWKS_PATH.equals(path) && !JWKS_ALIAS_PATH.equals(path);
    }
}
