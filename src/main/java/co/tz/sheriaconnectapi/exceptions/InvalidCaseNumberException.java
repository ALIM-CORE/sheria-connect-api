package co.tz.sheriaconnectapi.exceptions;

public class InvalidCaseNumberException extends RuntimeException {
    public InvalidCaseNumberException() {
        super(ErrorMessages.INVALID_CASE_NUMBER.getMessage());
    }
}
