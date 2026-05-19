package co.tz.sheriaconnectapi.exceptions;

public class ProviderProfileNotFoundException extends RuntimeException {
    public ProviderProfileNotFoundException() {
        super(ErrorMessages.PROVIDER_PROFILE_NOT_FOUND.getMessage());
    }
}
