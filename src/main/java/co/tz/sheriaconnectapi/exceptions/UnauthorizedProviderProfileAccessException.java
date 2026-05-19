package co.tz.sheriaconnectapi.exceptions;

public class UnauthorizedProviderProfileAccessException extends RuntimeException {
    public UnauthorizedProviderProfileAccessException() {
        super(ErrorMessages.UNAUTHORIZED_PROVIDER_PROFILE_ACCESS.getMessage());
    }
}
