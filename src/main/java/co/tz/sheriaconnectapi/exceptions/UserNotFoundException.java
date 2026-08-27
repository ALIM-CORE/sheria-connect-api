package co.tz.sheriaconnectapi.exceptions;

public class UserNotFoundException  extends DomainException{
    public UserNotFoundException() {
        super(ErrorMessages.MEMBER_NOT_FOUND);
    }
}
