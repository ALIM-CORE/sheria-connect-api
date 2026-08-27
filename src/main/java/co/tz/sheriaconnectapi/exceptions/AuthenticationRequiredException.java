package co.tz.sheriaconnectapi.exceptions;

public class AuthenticationRequiredException extends DomainException {
    public AuthenticationRequiredException() {
        super(ErrorMessages.AUTHENTICATION_REQUIRED);
    }
}
