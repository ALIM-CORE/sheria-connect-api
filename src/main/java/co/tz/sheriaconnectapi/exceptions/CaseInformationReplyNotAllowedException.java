package co.tz.sheriaconnectapi.exceptions;

public class CaseInformationReplyNotAllowedException extends DomainException {
    public CaseInformationReplyNotAllowedException() {
        super(ErrorMessages.CASE_INFORMATION_REPLY_NOT_ALLOWED);
    }
}
