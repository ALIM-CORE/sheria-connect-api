package co.tz.sheriaconnectapi.exceptions;

public class InvalidMatchingRequestStatusException extends RuntimeException {
    public InvalidMatchingRequestStatusException() {
        super(ErrorMessages.INVALID_MATCHING_REQUEST_STATUS.getMessage());
    }
}
