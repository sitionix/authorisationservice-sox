package com.sitionix.athssox.api.controller.handler;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.sitionix.athssox.api.ratelimit.RateLimitExceededException;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.InactiveUserException;
import com.sitionix.athssox.domain.exception.InvalidPasswordException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.exception.RefreshTokenExpiredException;
import com.sitionix.athssox.domain.exception.RefreshTokenInvalidException;
import com.sitionix.athssox.domain.exception.SessionMismatchException;
import com.sitionix.athssox.domain.exception.SessionNotActiveException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorDTO> handleEmailAlreadyRegistered(final EmailAlreadyRegisteredException exception) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorDTO> handleInvalidPassword(final InvalidPasswordException exception) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MissingSiteIdException.class)
    public ResponseEntity<ErrorDTO> handleMissingSiteId(final MissingSiteIdException exception) {
        return buildError(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ErrorDTO> handleAuthentication(final AuthenticationException exception) {
        return buildError(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
    }

    @ExceptionHandler(InactiveUserException.class)
    public ResponseEntity<ErrorDTO> handleInactiveUser(final InactiveUserException exception) {
        return buildError(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorDTO> handleRefreshTokenExpired(final RefreshTokenExpiredException exception) {
        return buildError(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler({RefreshTokenInvalidException.class, SessionNotActiveException.class, SessionMismatchException.class})
    public ResponseEntity<ErrorDTO> handleRefreshTokenForbidden(final RuntimeException exception) {
        return buildError(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidation(final MethodArgumentNotValidException exception) {
        final String details = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");

        return buildError(HttpStatus.BAD_REQUEST, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> handleNotReadable(final HttpMessageNotReadableException exception) {
        return buildError(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorDTO> handleRateLimitExceeded(final RateLimitExceededException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(ErrorDTO.builder()
                        .code(HttpStatus.TOO_MANY_REQUESTS.value())
                        .title(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .details(exception.getMessage())
                        .build());
    }

    private static ResponseEntity<ErrorDTO> buildError(final HttpStatus status, final String details) {
        return ResponseEntity.status(status)
                .body(ErrorDTO.builder()
                        .code(status.value())
                        .title(status.getReasonPhrase())
                        .details(details)
                        .build());
    }
}
