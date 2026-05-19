package co.tz.sheriaconnectapi.exceptions;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException() {
        super(ErrorMessages.INVALID_STATUS_TRANSITION.getMessage());
    }
}
