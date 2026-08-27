package co.tz.sheriaconnectapi.exceptions;

public class InvalidLoginCredentialsException extends DomainException {

    public InvalidLoginCredentialsException() {
        super(ErrorMessages.INVALID_LOGIN_CREDENTIALS);
    }
}
