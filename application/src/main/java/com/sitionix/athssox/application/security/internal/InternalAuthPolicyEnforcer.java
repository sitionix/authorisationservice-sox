package com.sitionix.athssox.application.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InternalAuthPolicyEnforcer {

    private static final String SCOPE_PREFIX = "scope:";
    private static final String ENDPOINT_PREFIX = "endpoint:";

    private final InternalAuthConfig internalAuthConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public void assertAllowed(final ServiceIdentity identity, final String requestPath) {
        if (identity == null || !StringUtils.hasText(identity.serviceName())) {
            throw new AccessDeniedException("Internal service identity missing");
        }
        final InternalAuthConfig.PolicyConfig policy = this.internalAuthConfig.getPolicies()
                .get(identity.serviceName());
        if (policy == null || policy.getAllow() == null || policy.getAllow().isEmpty()) {
            throw new AccessDeniedException("Internal service not allowed");
        }
        final boolean allowed = policy.getAllow().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(entry -> this.matches(entry, identity, requestPath));
        if (!allowed) {
            throw new AccessDeniedException("Internal service not allowed");
        }
    }

    private boolean matches(final String entry, final ServiceIdentity identity, final String requestPath) {
        if (this.isScopeEntry(entry)) {
            final String scope = this.normalizeScope(entry);
            if ("*".equals(scope)) {
                return true;
            }
            final List<String> scopes = identity.scopes() == null ? List.of() : identity.scopes();
            return scopes.stream()
                    .filter(Objects::nonNull)
                    .anyMatch(scope::equals);
        }
        final String pattern = this.normalizeEndpoint(entry);
        return StringUtils.hasText(requestPath) && this.pathMatcher.match(pattern, requestPath);
    }

    private boolean isScopeEntry(final String entry) {
        if (entry.startsWith(SCOPE_PREFIX)) {
            return true;
        }
        if (entry.startsWith(ENDPOINT_PREFIX)) {
            return false;
        }
        return !entry.startsWith("/");
    }

    private String normalizeScope(final String entry) {
        if (entry.startsWith(SCOPE_PREFIX)) {
            return entry.substring(SCOPE_PREFIX.length());
        }
        return entry;
    }

    private String normalizeEndpoint(final String entry) {
        if (entry.startsWith(ENDPOINT_PREFIX)) {
            return entry.substring(ENDPOINT_PREFIX.length());
        }
        return entry;
    }
}
