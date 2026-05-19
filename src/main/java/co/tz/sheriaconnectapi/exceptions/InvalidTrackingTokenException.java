package co.tz.sheriaconnectapi.exceptions;

public class InvalidTrackingTokenException extends RuntimeException {
    public InvalidTrackingTokenException() {
        super(ErrorMessages.INVALID_TRACKING_TOKEN.getMessage());
    }
}
