package com.sitionix.athssox.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import com.sitionix.athssox.application.security.internal.DevJwtServiceIdentityVerifier;
import com.sitionix.athssox.application.security.internal.InternalAuthMode;
import com.sitionix.athssox.application.security.internal.InternalAuthPolicyEnforcer;
import com.sitionix.athssox.application.security.internal.ServiceIdentity;
import com.sitionix.athssox.application.security.internal.ServiceIdentityAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalAuthFilter extends OncePerRequestFilter {

    public static final String SERVICE_IDENTITY_ATTRIBUTE = "internalServiceIdentity";

    private final InternalAuthConfig internalAuthConfig;
    private final DevJwtServiceIdentityVerifier devJwtServiceIdentityVerifier;
    private final InternalAuthPolicyEnforcer internalAuthPolicyEnforcer;
    private final MtlsServiceIdentityVerifier mtlsServiceIdentityVerifier;

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        try {
            final ServiceIdentity identity = this.authenticate(request);
            this.authorize(identity, request);
            request.setAttribute(SERVICE_IDENTITY_ATTRIBUTE, identity);
            SecurityContextHolder.getContext()
                    .setAuthentication(ServiceIdentityAuthenticationToken.authenticated(identity));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private ServiceIdentity authenticate(final HttpServletRequest request) {
        final InternalAuthMode mode = this.internalAuthConfig.getMode();
        if (mode == null) {
            throw new AuthenticationCredentialsNotFoundException("Internal auth mode is not configured");
        }
        if (mode == InternalAuthMode.DEV_JWT) {
            final String token = this.extractBearerToken(request);
            return this.devJwtServiceIdentityVerifier.verify(token);
        }
        if (mode == InternalAuthMode.MTLS) {
            return this.mtlsServiceIdentityVerifier.verify(request);
        }
        throw new AuthenticationCredentialsNotFoundException("Unsupported internal auth mode");
    }

    private void authorize(final ServiceIdentity identity, final HttpServletRequest request) {
        final String requestPath = InternalAuthRequestHelper.resolvePath(request);
        this.internalAuthPolicyEnforcer.assertAllowed(identity, request.getMethod(), requestPath);
    }

    private String extractBearerToken(final HttpServletRequest request) {
        final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            throw new AuthenticationCredentialsNotFoundException("Missing internal Authorization header");
        }
        if (!header.startsWith("Bearer ")) {
            throw new BadCredentialsException("Invalid Authorization header");
        }
        final String token = header.substring("Bearer ".length()).trim();
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Invalid Authorization header");
        }
        return token;
    }
}
