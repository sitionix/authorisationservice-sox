package com.sitionix.athssox.config;

import com.sitionix.athssox.application.security.LoginAuthenticationProvider;
import com.sitionix.athssox.security.internal.InternalAuthFilter;
import com.sitionix.athssox.security.internal.InternalEndpointRequestMatcher;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/email/verify",
            "/api/v1/auth/refresh",
            "/api/v1/users",
            "/.well-known/jwks.json",
            "/oauth2/v1/keys"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(final HttpSecurity http,
                                                           final SecurityErrorHandler securityErrorHandler,
                                                           final InternalAuthFilter internalAuthFilter,
                                                           final InternalEndpointRequestMatcher internalEndpointRequestMatcher)
            throws Exception {
        return http
                .securityMatcher(internalEndpointRequestMatcher)
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .addFilterBefore(internalAuthFilter, AuthorizationFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(final HttpSecurity http,
                                                   final SecurityErrorHandler securityErrorHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler))
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(final LoginAuthenticationProvider loginAuthenticationProvider) {
        return new ProviderManager(List.of(loginAuthenticationProvider));
    }

    @Bean
    public FilterRegistrationBean<InternalAuthFilter> internalAuthFilterRegistration(
            final InternalAuthFilter internalAuthFilter) {
        final FilterRegistrationBean<InternalAuthFilter> registration = new FilterRegistrationBean<>(internalAuthFilter);
        registration.setEnabled(false);
        return registration;
    }
}
