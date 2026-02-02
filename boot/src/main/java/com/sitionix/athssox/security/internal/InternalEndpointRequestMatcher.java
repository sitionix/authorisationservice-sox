package com.sitionix.athssox.security.internal;

import com.sitionix.athssox.application.config.InternalAuthConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InternalEndpointRequestMatcher implements RequestMatcher {

    private final InternalAuthConfig internalAuthConfig;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean matches(final HttpServletRequest request) {
        final List<String> patterns = this.internalAuthConfig.getProtectedEndpoints();
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        final String path = InternalAuthRequestHelper.resolvePath(request);
        return patterns.stream()
                .filter(StringUtils::hasText)
                .anyMatch(pattern -> this.pathMatcher.match(pattern, path));
    }
}
