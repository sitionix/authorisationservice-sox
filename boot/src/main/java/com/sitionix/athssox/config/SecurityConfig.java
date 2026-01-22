package com.sitionix.athssox.config;

import com.sitionix.athssox.application.security.LoginAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/email/verify",
            "/api/v1/auth/emailVerificationTokens/**:issueLink",
            "/api/v1/auth/refresh",
            "/api/v1/users",
            "/.well-known/jwks.json",
            "/oauth2/jwks"
    };

    @Bean
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
}
