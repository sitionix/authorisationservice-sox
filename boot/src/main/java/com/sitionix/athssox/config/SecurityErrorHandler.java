package com.sitionix.athssox.config;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(final HttpServletRequest request,
                         final HttpServletResponse response,
                         final AuthenticationException exception) throws IOException {
        this.writeError(response, HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @Override
    public void handle(final HttpServletRequest request,
                       final HttpServletResponse response,
                       final AccessDeniedException exception) throws IOException {
        this.writeError(response, HttpStatus.FORBIDDEN, exception.getMessage());
    }

    private void writeError(final HttpServletResponse response,
                            final HttpStatus status,
                            final String details) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        final ErrorDTO body = ErrorDTO.builder()
                .code(status.value())
                .title(status.getReasonPhrase())
                .details(details == null ? status.getReasonPhrase() : details)
                .build();

        this.objectMapper.writeValue(response.getWriter(), body);
    }
}
