package co.tz.sheriaconnectapi.exceptions;

public class MatchingRequestNotFoundException extends RuntimeException {
    public MatchingRequestNotFoundException() {
        super(ErrorMessages.MATCHING_REQUEST_NOT_FOUND.getMessage());
    }
}
