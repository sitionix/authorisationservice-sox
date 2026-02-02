package com.sitionix.athssox.application.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class InternalAuthPolicyEnforcer {

    private static final String SCOPE_PREFIX = "scope:";
    private static final String ENDPOINT_PREFIX = "endpoint:";
    private static final String ALLOW_ALL = "*";

    private final InternalAuthConfig internalAuthConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public void assertAllowed(final ServiceIdentity identity, final String requestMethod, final String requestPath) {
        if (identity == null || !StringUtils.hasText(identity.serviceName())) {
            throw new AccessDeniedException("Internal service identity missing");
        }
        final InternalAuthConfig.PolicyConfig policy = this.internalAuthConfig.getPolicies()
                .get(identity.serviceName());
        if (policy == null || policy.getAllow() == null || policy.getAllow().isEmpty()) {
            throw new AccessDeniedException("Internal service not allowed");
        }
        final String normalizedMethod = requestMethod == null ? "" : requestMethod.trim().toUpperCase(Locale.ROOT);
        final boolean allowed = policy.getAllow().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(entry -> this.matches(entry, identity, normalizedMethod, requestPath));
        if (!allowed) {
            throw new AccessDeniedException("Internal service not allowed");
        }
    }

    private boolean matches(final String entry,
                            final ServiceIdentity identity,
                            final String requestMethod,
                            final String requestPath) {
        if (ALLOW_ALL.equals(entry)) {
            return true;
        }
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
        final EndpointPolicy endpointPolicy = this.parseEndpointPolicy(entry);
        if (endpointPolicy.method == null) {
            return false;
        }
        if (!endpointPolicy.method.equals(requestMethod)) {
            return false;
        }
        return StringUtils.hasText(requestPath) && this.pathMatcher.match(endpointPolicy.path, requestPath);
    }

    private boolean isScopeEntry(final String entry) {
        return entry.startsWith(SCOPE_PREFIX);
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

    private EndpointPolicy parseEndpointPolicy(final String entry) {
        final String normalized = this.normalizeEndpoint(entry);
        final String methodFromEntry = this.extractMethod(normalized);
        if (methodFromEntry == null) {
            return new EndpointPolicy(null, normalized);
        }
        final String path = normalized.substring(methodFromEntry.length() + 1).trim();
        return new EndpointPolicy(methodFromEntry, path);
    }

    private String extractMethod(final String entry) {
        final String trimmed = entry.trim();
        final String upper = trimmed.toUpperCase(Locale.ROOT);
        for (final String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"}) {
            if (upper.startsWith(method + " ")) {
                return method;
            }
            if (upper.startsWith(method + ":")) {
                return method;
            }
        }
        return null;
    }

    private static final class EndpointPolicy {

        private final String method;
        private final String path;

        private EndpointPolicy(final String method, final String path) {
            this.method = method;
            this.path = path;
        }
    }
}
