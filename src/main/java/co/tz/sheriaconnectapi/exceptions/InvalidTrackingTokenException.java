package co.tz.sheriaconnectapi.exceptions;

public class InvalidTrackingTokenException extends DomainException {
    public InvalidTrackingTokenException() {
        super(ErrorMessages.INVALID_TRACKING_TOKEN);
    }
}
