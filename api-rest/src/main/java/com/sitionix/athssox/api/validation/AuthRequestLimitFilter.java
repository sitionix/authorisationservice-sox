package com.sitionix.athssox.api.validation;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AuthRequestLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTER_PATH = "/api/v1/users";
    private static final String REFRESH_PATH = "/api/v1/auth/refresh";

    private static final String INVALID_REQUEST_MESSAGE = "Invalid request payload";
    private static final String MALFORMED_REQUEST_MESSAGE = "Malformed request body";
    private static final String REQUEST_TOO_LARGE_MESSAGE = "Request body too large";

    private static final Pattern BASIC_EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private final ObjectMapper objectMapper;
    private final AuthRequestLimitProperties requestLimitProperties;

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        final String uri = request.getRequestURI();
        if (!StringUtils.hasText(uri)) {
            return true;
        }
        return !isTargetEndpoint(uri);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {
        final long maxBodyBytes = this.requestLimitProperties.getMaxBodySize().toBytes();
        final long contentLength = request.getContentLengthLong();
        if (contentLength > maxBodyBytes) {
            writeError(response, HttpStatus.PAYLOAD_TOO_LARGE, REQUEST_TOO_LARGE_MESSAGE);
            return;
        }

        final CachedBodyHttpServletRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyHttpServletRequest(request, maxBodyBytes);
        } catch (final RequestBodyTooLargeException ex) {
            writeError(response, HttpStatus.PAYLOAD_TOO_LARGE, REQUEST_TOO_LARGE_MESSAGE);
            return;
        }

        if (!isJsonRequest(cachedRequest)) {
            filterChain.doFilter(cachedRequest, response);
            return;
        }

        final JsonNode root;
        try {
            root = this.objectMapper.readTree(cachedRequest.getCachedBody());
        } catch (final IOException ex) {
            writeError(response, HttpStatus.BAD_REQUEST, MALFORMED_REQUEST_MESSAGE);
            return;
        }

        if (root == null || root.isNull() || root.isMissingNode()) {
            writeError(response, HttpStatus.BAD_REQUEST, MALFORMED_REQUEST_MESSAGE);
            return;
        }

        final Optional<String> validationError = validate(root, cachedRequest.getRequestURI());
        if (validationError.isPresent()) {
            writeError(response, HttpStatus.BAD_REQUEST, validationError.get());
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private Optional<String> validate(final JsonNode root, final String uri) {
        if (uri.endsWith(LOGIN_PATH)) {
            return validateLogin(root);
        }
        if (uri.endsWith(REGISTER_PATH)) {
            return validateRegister(root);
        }
        if (uri.endsWith(REFRESH_PATH)) {
            return validateRefresh(root);
        }
        return Optional.empty();
    }

    private Optional<String> validateLogin(final JsonNode root) {
        final String email = readText(root, "email");
        if (!isValidRequiredEmail(email)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        final String password = readText(root, "password");
        if (!isValidRequiredPassword(password)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        final String sessionSourceId = readText(root, "sessionSourceId");
        if (StringUtils.hasText(sessionSourceId) && sessionSourceId.length() > this.requestLimitProperties.getSessionSourceIdMaxLength()) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        if (sessionSourceId != null && !StringUtils.hasText(sessionSourceId)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        return Optional.empty();
    }

    private Optional<String> validateRegister(final JsonNode root) {
        final String email = readText(root, "email");
        if (!isValidRequiredEmail(email)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        final String password = readText(root, "password");
        if (!isValidRequiredPassword(password)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        return Optional.empty();
    }

    private Optional<String> validateRefresh(final JsonNode root) {
        final String sessionSourceId = readText(root, "sessionSourceId");
        if (StringUtils.hasText(sessionSourceId) && sessionSourceId.length() > this.requestLimitProperties.getSessionSourceIdMaxLength()) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        if (sessionSourceId != null && !StringUtils.hasText(sessionSourceId)) {
            return Optional.of(INVALID_REQUEST_MESSAGE);
        }
        return Optional.empty();
    }

    private boolean isValidRequiredEmail(final String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        if (email.length() > this.requestLimitProperties.getEmailMaxLength()) {
            return false;
        }
        return BASIC_EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidRequiredPassword(final String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        return password.length() <= this.requestLimitProperties.getPasswordMaxLength();
    }

    private static String readText(final JsonNode root, final String field) {
        final JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            return "";
        }
        return node.asText();
    }

    private static boolean isTargetEndpoint(final String uri) {
        return uri.endsWith(LOGIN_PATH)
                || uri.endsWith(REGISTER_PATH)
                || uri.endsWith(REFRESH_PATH);
    }

    private static boolean isJsonRequest(final HttpServletRequest request) {
        final String contentType = request.getContentType();
        return contentType == null || contentType.startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    private void writeError(final HttpServletResponse response,
                            final HttpStatus status,
                            final String details) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        final ErrorDTO error = ErrorDTO.builder()
                .code(status.value())
                .title(status.getReasonPhrase())
                .details(details)
                .build();
        this.objectMapper.writeValue(response.getOutputStream(), error);
    }
}
