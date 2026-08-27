package co.tz.sheriaconnectapi.exceptions;

public class EmailAlreadyVerifiedException
        extends DomainException {

    public EmailAlreadyVerifiedException() {
        super(ErrorMessages.EMAIL_ALREADY_VERIFIED);
    }
}
