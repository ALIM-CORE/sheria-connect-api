package co.tz.sheriaconnectapi.exceptions;

public class InvalidCaseNumberException extends DomainException {
    public InvalidCaseNumberException() {
        super(ErrorMessages.INVALID_CASE_NUMBER);
    }
}
