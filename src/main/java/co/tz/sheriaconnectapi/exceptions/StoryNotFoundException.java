package co.tz.sheriaconnectapi.exceptions;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException() {
        super(ErrorMessages.STORY_NOT_FOUND.getMessage());
    }
}
