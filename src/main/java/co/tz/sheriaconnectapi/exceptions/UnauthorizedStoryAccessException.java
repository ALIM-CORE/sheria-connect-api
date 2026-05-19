package co.tz.sheriaconnectapi.exceptions;

public class UnauthorizedStoryAccessException extends RuntimeException {
    public UnauthorizedStoryAccessException() {
        super(ErrorMessages.UNAUTHORIZED_STORY_ACCESS.getMessage());
    }
}
