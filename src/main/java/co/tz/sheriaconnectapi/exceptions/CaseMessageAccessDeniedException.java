package co.tz.sheriaconnectapi.exceptions;

public class CaseMessageAccessDeniedException extends DomainException {
    public CaseMessageAccessDeniedException() {
        super(ErrorMessages.CASE_MESSAGE_NOT_ALLOWED);
    }
}
