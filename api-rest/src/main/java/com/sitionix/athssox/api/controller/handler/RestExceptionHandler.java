package com.sitionix.athssox.api.controller.handler;

import com.app_afesox.athssox.api_first.dto.ErrorDTO;
import com.sitionix.athssox.domain.exception.EmailAlreadyRegisteredException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenExpiredException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenInvalidException;
import com.sitionix.athssox.domain.exception.EmailVerificationTokenNotFoundException;
import com.sitionix.athssox.domain.exception.InactiveUserException;
import com.sitionix.athssox.domain.exception.InvalidPasswordException;
import com.sitionix.athssox.domain.exception.MissingSiteIdException;
import com.sitionix.athssox.domain.exception.RefreshTokenExpiredException;
import com.sitionix.athssox.domain.exception.RefreshTokenInvalidException;
import com.sitionix.athssox.domain.exception.SessionMismatchException;
import com.sitionix.athssox.domain.exception.SessionNotActiveException;
import com.sitionix.athssox.domain.exception.UserAlreadyVerifiedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    @ExceptionHandler(EmailVerificationTokenNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleEmailVerificationTokenNotFound(final EmailVerificationTokenNotFoundException exception) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(EmailVerificationTokenExpiredException.class)
    public ResponseEntity<ErrorDTO> handleEmailVerificationTokenExpired(final EmailVerificationTokenExpiredException exception) {
        return buildError(HttpStatus.GONE, exception.getMessage());
    }

    @ExceptionHandler(EmailVerificationTokenInvalidException.class)
    public ResponseEntity<ErrorDTO> handleEmailVerificationTokenInvalid(final EmailVerificationTokenInvalidException exception) {
        return buildError(HttpStatus.GONE, exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyVerifiedException.class)
    public ResponseEntity<ErrorDTO> handleUserAlreadyVerified(final UserAlreadyVerifiedException exception) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage());
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> handleTypeMismatch(final MethodArgumentTypeMismatchException exception) {
        return buildError(HttpStatus.BAD_REQUEST, "Invalid request parameter");
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
