package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidTokenException extends DomainException {
    public InvalidTokenException(ErrorMessages error) {
        super(error);
    }

    public InvalidTokenException(String message) {
        super(ErrorMessages.INVALID_TOKEN, message);
    }
}
