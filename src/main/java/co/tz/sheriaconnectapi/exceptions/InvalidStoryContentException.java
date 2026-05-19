package co.tz.sheriaconnectapi.exceptions;

public class InvalidStoryContentException extends RuntimeException {
    public InvalidStoryContentException(String message) {
        super(message == null || message.isBlank()
                ? ErrorMessages.INVALID_STORY_CONTENT.getMessage()
                : message);
    }
}
