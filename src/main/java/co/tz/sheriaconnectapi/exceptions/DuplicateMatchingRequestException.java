package co.tz.sheriaconnectapi.exceptions;

public class DuplicateMatchingRequestException extends RuntimeException {
    public DuplicateMatchingRequestException() {
        super(ErrorMessages.DUPLICATE_MATCHING_REQUEST.getMessage());
    }
}
