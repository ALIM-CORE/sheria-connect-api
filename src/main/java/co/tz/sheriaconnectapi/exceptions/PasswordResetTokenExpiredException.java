package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class PasswordResetTokenExpiredException
        extends RuntimeException {

    public PasswordResetTokenExpiredException() {
        super(ErrorMessages.PASSWORD_RESET_TOKEN_EXPIRED.getMessage());
    }
}
