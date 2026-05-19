package co.tz.sheriaconnectapi.exceptions;

public class StoryNotPublishedException extends RuntimeException {
    public StoryNotPublishedException() {
        super(ErrorMessages.STORY_NOT_PUBLISHED.getMessage());
    }
}
