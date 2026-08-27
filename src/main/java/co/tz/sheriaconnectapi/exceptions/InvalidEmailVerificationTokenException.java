package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidEmailVerificationTokenException
        extends DomainException {

    public InvalidEmailVerificationTokenException() {
        super(ErrorMessages.EMAIL_VERIFICATION_TOKEN_INVALID);
    }
}
