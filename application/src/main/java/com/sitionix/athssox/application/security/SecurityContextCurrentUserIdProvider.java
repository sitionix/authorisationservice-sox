package com.sitionix.athssox.application.security;

import com.sitionix.athssox.domain.exception.AccessTokenAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component
public class SecurityContextCurrentUserIdProvider implements CurrentUserIdProvider {

    @Override
    public Long currentUserId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Objects.isNull(authentication)) {
            throw new AccessTokenAuthenticationException("Authentication required.");
        }
        final Object details = authentication.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        if (details instanceof Number number) {
            return number.longValue();
        }
        if (details instanceof String subject && StringUtils.hasText(subject)) {
            try {
                return Long.valueOf(subject);
            } catch (final NumberFormatException ex) {
                throw new AccessTokenAuthenticationException("Authentication required.");
            }
        }
        throw new AccessTokenAuthenticationException("Authentication required.");
    }
}
