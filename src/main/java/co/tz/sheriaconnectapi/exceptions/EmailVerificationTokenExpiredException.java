package co.tz.sheriaconnectapi.exceptions;

public class EmailVerificationTokenExpiredException
        extends RuntimeException {

    public EmailVerificationTokenExpiredException() {
        super(ErrorMessages.EMAIL_VERIFICATION_TOKEN_EXPIRED.getMessage());
    }
}
