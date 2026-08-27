package co.tz.sheriaconnectapi.exceptions;

public class StoryNotPublishedException extends DomainException {
    public StoryNotPublishedException() {
        super(ErrorMessages.STORY_NOT_PUBLISHED);
    }
}
