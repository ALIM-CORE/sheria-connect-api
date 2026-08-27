package co.tz.sheriaconnectapi.exceptions;

import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void domainExceptionResponsesIncludeStableCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleInvalidLoginCredentialsException(
                        new InvalidLoginCredentialsException()
                );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.INVALID_LOGIN_CREDENTIALS);
    }

    @Test
    void dynamicValidationResponseKeepsMessageAndUsesFamilyCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleUserNotValidException(
                        new UserNotValidException("Message body is required")
                );

        StandardResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Message body is required", body.getError());
        assertEquals(ErrorMessages.USER_NOT_VALID.name(), body.getCode());
    }

    @Test
    void specificValidationCodeCanBeReturnedWhenKnown() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleUserNotValidException(
                        new UserNotValidException(ErrorMessages.EMAIL_ALREADY_EXISTS)
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.EMAIL_ALREADY_EXISTS);
    }

    @Test
    void unreadableRequestUsesInvalidPayloadCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleUnreadableRequest(
                        new HttpMessageNotReadableException(
                                "bad payload",
                                (HttpInputMessage) null
                        )
                );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.INVALID_REQUEST_PAYLOAD);
    }

    @Test
    void frameworkAccessDeniedUsesAccessDeniedCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleAccessDenied(new AccessDeniedException("forbidden"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.ACCESS_DENIED);
    }

    @Test
    void selfReviewDenialUsesSpecificCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleUnauthorizedCaseAccess(
                        new UnauthorizedCaseAccessException(
                                ErrorMessages.STAFF_SELF_REVIEW_DENIED
                        )
                );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.STAFF_SELF_REVIEW_DENIED);
    }

    @Test
    void unexpectedErrorsUseUnexpectedErrorCode() {
        ResponseEntity<StandardResponse<Void>> response =
                handler.handleGenericException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertError(response.getBody(), ErrorMessages.UNEXPECTED_ERROR);
    }

    private void assertError(StandardResponse<Void> body, ErrorMessages error) {
        assertNotNull(body);
        assertEquals(false, body.isSuccess());
        assertEquals(error.getMessage(), body.getError());
        assertEquals(error.name(), body.getCode());
    }
}
