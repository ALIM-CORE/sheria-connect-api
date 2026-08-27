package co.tz.sheriaconnectapi.exceptions;

public class EmailVerificationTokenExpiredException
        extends DomainException {

    public EmailVerificationTokenExpiredException() {
        super(ErrorMessages.EMAIL_VERIFICATION_TOKEN_EXPIRED);
    }
}
