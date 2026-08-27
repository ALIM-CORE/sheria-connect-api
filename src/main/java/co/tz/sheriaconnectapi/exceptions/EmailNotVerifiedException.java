package co.tz.sheriaconnectapi.exceptions;

public class EmailNotVerifiedException
        extends DomainException{

    public EmailNotVerifiedException() {
        super(ErrorMessages.EMAIL_NOT_VERIFIED);
    }
}
