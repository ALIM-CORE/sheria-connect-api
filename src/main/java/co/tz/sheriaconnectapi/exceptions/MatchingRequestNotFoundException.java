package co.tz.sheriaconnectapi.exceptions;

public class MatchingRequestNotFoundException extends DomainException {
    public MatchingRequestNotFoundException() {
        super(ErrorMessages.MATCHING_REQUEST_NOT_FOUND);
    }
}
