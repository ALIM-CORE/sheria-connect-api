package co.tz.sheriaconnectapi.exceptions;

public class UnauthorizedCaseAccessException extends DomainException {
    public UnauthorizedCaseAccessException() {
        super(ErrorMessages.UNAUTHORIZED_CASE_ACCESS);
    }

    public UnauthorizedCaseAccessException(ErrorMessages error) {
        super(error);
    }
}
