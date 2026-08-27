package co.tz.sheriaconnectapi.exceptions;

public class InvalidStatusTransitionException extends DomainException {
    public InvalidStatusTransitionException() {
        super(ErrorMessages.INVALID_STATUS_TRANSITION);
    }
}
