package co.tz.sheriaconnectapi.exceptions;

public class InvalidStoryContentException extends DomainException {
    public InvalidStoryContentException(String message) {
        super(ErrorMessages.INVALID_STORY_CONTENT, message);
    }
}
