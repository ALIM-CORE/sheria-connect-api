package co.tz.sheriaconnectapi.exceptions;

public class UnauthorizedCaseAccessException extends RuntimeException {
    public UnauthorizedCaseAccessException() {
        super(ErrorMessages.UNAUTHORIZED_CASE_ACCESS.getMessage());
    }
}
