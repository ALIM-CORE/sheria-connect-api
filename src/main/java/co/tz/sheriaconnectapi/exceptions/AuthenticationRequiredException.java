package co.tz.sheriaconnectapi.exceptions;

public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("Authentication is required");
    }
}
