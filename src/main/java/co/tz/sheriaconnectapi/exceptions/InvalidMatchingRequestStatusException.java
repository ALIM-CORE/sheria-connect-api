package co.tz.sheriaconnectapi.exceptions;

public class InvalidMatchingRequestStatusException extends DomainException {
    public InvalidMatchingRequestStatusException() {
        super(ErrorMessages.INVALID_MATCHING_REQUEST_STATUS);
    }
}
