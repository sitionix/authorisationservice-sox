package com.sitionix.athssox.api.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final SecurityProperties securityProperties;

    public ClientIpResolver(final SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String resolve(final HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        if (this.securityProperties.isTrustedProxy()) {
            final String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);
            if (StringUtils.hasText(forwardedFor)) {
                final String[] parts = forwardedFor.split(",");
                if (parts.length > 0 && StringUtils.hasText(parts[0])) {
                    return parts[0].trim();
                }
            }
        }

        final String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : "unknown";
    }
}
