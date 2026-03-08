package co.tz.sheriaconnectapi.exceptions;

public class UserNotFoundException  extends RuntimeException{
    public UserNotFoundException() {
        super(ErrorMessages.MEMBER_NOT_FOUND.getMessage());
    }
}
