package co.tz.sheriaconnectapi.exceptions;

import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleUserNotFoundException(
            UserNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleRoleNotFoundException(
            RoleNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthorityNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleAuthorityNotFoundException(
            AuthorityNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserNotValidException.class)
    public ResponseEntity<StandardResponse<Void>> handleUserNotValidException(
            UserNotValidException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidLoginCredentialsException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidLoginCredentialsException(
            InvalidLoginCredentialsException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({
            AccessDeniedException.class,
            AuthorizationDeniedException.class
    })
    public ResponseEntity<StandardResponse<Void>> handleAccessDenied(
            RuntimeException ex
    ) {
        return ResponseUtil.error("Access denied", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(InvalidClientTypeException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidClientTypeException(
            InvalidClientTypeException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidTokenException(
            InvalidTokenException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidEmailVerificationTokenException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidEmailVerificationToken(
            InvalidEmailVerificationTokenException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailVerificationTokenExpiredException.class)
    public ResponseEntity<StandardResponse<Void>> handleExpiredVerificationToken(
            EmailVerificationTokenExpiredException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<StandardResponse<Void>> handleAlreadyVerified(
            EmailAlreadyVerifiedException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<StandardResponse<Void>> handleEmailNotVerified(
            EmailNotVerifiedException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PasswordResetTokenExpiredException.class)
    public ResponseEntity<StandardResponse<Void>> handleExpiredPasswordResetToken(
            PasswordResetTokenExpiredException ex
    ){
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IncidentReportNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleIncidentReportNotFound(
            IncidentReportNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProviderProfileNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleProviderProfileNotFound(
            ProviderProfileNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MatchingRequestNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleMatchingRequestNotFound(
            MatchingRequestNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(StoryNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleStoryNotFound(
            StoryNotFoundException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidCaseNumberException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidCaseNumber(
            InvalidCaseNumberException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTrackingTokenException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidTrackingToken(
            InvalidTrackingTokenException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthorizedCaseAccessException.class)
    public ResponseEntity<StandardResponse<Void>> handleUnauthorizedCaseAccess(
            UnauthorizedCaseAccessException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            UnauthorizedStoryAccessException.class,
            StoryNotPublishedException.class
    })
    public ResponseEntity<StandardResponse<Void>> handleStoryAccessDenied(
            RuntimeException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            MissingEvidenceFileException.class,
            UnsupportedEvidenceTypeException.class,
            InvalidStatusTransitionException.class,
            InvalidMatchingRequestStatusException.class,
            InvalidStoryModerationStatusException.class,
            InvalidStoryContentException.class
    })
    public ResponseEntity<StandardResponse<Void>> handleBadCaseRequest(
            RuntimeException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateMatchingRequestException.class)
    public ResponseEntity<StandardResponse<Void>> handleDuplicateMatchingRequest(
            DuplicateMatchingRequestException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EvidenceFileTooLargeException.class)
    public ResponseEntity<StandardResponse<Void>> handleEvidenceFileTooLarge(
            EvidenceFileTooLargeException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(EvidenceStorageException.class)
    public ResponseEntity<StandardResponse<Void>> handleEvidenceStorage(
            EvidenceStorageException ex
    ) {
        return ResponseUtil.error(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Fallback handler to ensure ALL unexpected errors
     * still return the standard API response format.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<Void>> handleGenericException(
            Exception ex
    ) {
        return ResponseUtil.error(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}
