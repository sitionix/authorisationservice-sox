package com.sitionix.athssox.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Configuration
@Profile("it")
public class TestUserAgentHeaderConfig {

    private static final String DEFAULT_USER_AGENT = "forge-it";

    @Bean
    public Filter userAgentHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(final HttpServletRequest request,
                                            final HttpServletResponse response,
                                            final FilterChain filterChain) throws ServletException, IOException {
                final String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
                if (userAgent != null && !userAgent.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                filterChain.doFilter(new UserAgentHeaderRequestWrapper(request), response);
            }
        };
    }

    private static final class UserAgentHeaderRequestWrapper extends HttpServletRequestWrapper {

        private UserAgentHeaderRequestWrapper(final HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getHeader(final String name) {
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(name)) {
                return DEFAULT_USER_AGENT;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(final String name) {
            if (HttpHeaders.USER_AGENT.equalsIgnoreCase(name)) {
                return Collections.enumeration(List.of(DEFAULT_USER_AGENT));
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            final List<String> names = Collections.list(super.getHeaderNames());
            if (!names.contains(HttpHeaders.USER_AGENT)) {
                names.add(HttpHeaders.USER_AGENT);
            }
            return Collections.enumeration(new ArrayList<>(names));
        }
    }
}
