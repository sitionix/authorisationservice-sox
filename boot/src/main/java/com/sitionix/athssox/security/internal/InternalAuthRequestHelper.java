package com.sitionix.athssox.security.internal;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

final class InternalAuthRequestHelper {

    private InternalAuthRequestHelper() {
    }

    static String resolvePath(final HttpServletRequest request) {
        String path = request.getRequestURI();
        final String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return path;
    }
}
