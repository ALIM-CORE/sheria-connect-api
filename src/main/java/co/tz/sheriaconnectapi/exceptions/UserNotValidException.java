package co.tz.sheriaconnectapi.exceptions;

public class UserNotValidException extends DomainException {
    public UserNotValidException(ErrorMessages error) {
        super(error);
    }

    public UserNotValidException(String message) {
        super(ErrorMessages.USER_NOT_VALID, message);
    }
}
