package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class AuthorityNotFoundException extends RuntimeException {
    public AuthorityNotFoundException() {
        super(ErrorMessages.AUTHORITY_NOT_FOUND.getMessage());
    }
}