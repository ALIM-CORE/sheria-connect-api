package co.tz.sheriaconnectapi.exceptions;

public class DuplicateCaseInformationReplyException extends DomainException {
    public DuplicateCaseInformationReplyException() {
        super(ErrorMessages.DUPLICATE_CASE_INFORMATION_REPLY);
    }
}
