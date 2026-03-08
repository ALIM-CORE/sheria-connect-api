package co.tz.sheriaconnectapi.exceptions;

public class EmailAlreadyVerifiedException
        extends RuntimeException {

    public EmailAlreadyVerifiedException() {
        super(ErrorMessages.EMAIL_ALREADY_VERIFIED.getMessage());
    }
}
