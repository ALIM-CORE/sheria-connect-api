package co.tz.sheriaconnectapi.exceptions;

public class UnauthorizedStoryAccessException extends DomainException {
    public UnauthorizedStoryAccessException() {
        super(ErrorMessages.UNAUTHORIZED_STORY_ACCESS);
    }
}
