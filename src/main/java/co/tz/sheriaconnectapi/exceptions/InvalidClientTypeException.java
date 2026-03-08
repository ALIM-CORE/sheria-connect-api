package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidClientTypeException extends RuntimeException {
    public InvalidClientTypeException() {
        super(ErrorMessages.INVALID_CLIENT_TYPE.getMessage());
    }
}
