package co.tz.sheriaconnectapi.exceptions;

public class InvalidCaseInformationReplyException extends DomainException {
    public InvalidCaseInformationReplyException() {
        super(ErrorMessages.INVALID_CASE_INFORMATION_REPLY);
    }
}
