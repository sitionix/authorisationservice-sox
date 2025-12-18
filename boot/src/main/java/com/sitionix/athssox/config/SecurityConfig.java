package com.sitionix.athssox.config;

import com.sitionix.athssox.application.security.LoginAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                // Enforce CSRF only when a client sends cookies (cookie-based auth).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .requireCsrfProtectionMatcher(request -> {
                            if (request.getHeader("Cookie") == null) {
                                return false;
                            }
                            final String method = request.getMethod();
                            return !(HttpMethod.GET.matches(method)
                                    || HttpMethod.HEAD.matches(method)
                                    || HttpMethod.OPTIONS.matches(method)
                                    || HttpMethod.TRACE.matches(method));
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/login", "/api/v1/users").permitAll()
                        .anyRequest().authenticated())
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(final LoginAuthenticationProvider loginAuthenticationProvider) {
        return new ProviderManager(List.of(loginAuthenticationProvider));
    }
}
