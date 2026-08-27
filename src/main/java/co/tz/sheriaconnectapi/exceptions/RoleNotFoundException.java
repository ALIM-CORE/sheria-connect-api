package co.tz.sheriaconnectapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class RoleNotFoundException extends DomainException {
    public RoleNotFoundException() {
        super(ErrorMessages.ROLE_NOT_FOUND);
    }
}
