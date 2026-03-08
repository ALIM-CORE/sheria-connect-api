package co.tz.sheriaconnectapi.exceptions;

public class UserNotValidException extends RuntimeException {
    public UserNotValidException(String message) {
        super(message);
    }
}
