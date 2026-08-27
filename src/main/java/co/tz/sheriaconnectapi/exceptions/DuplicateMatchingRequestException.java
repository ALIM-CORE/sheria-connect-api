package co.tz.sheriaconnectapi.exceptions;

public class DuplicateMatchingRequestException extends DomainException {
    public DuplicateMatchingRequestException() {
        super(ErrorMessages.DUPLICATE_MATCHING_REQUEST);
    }
}
