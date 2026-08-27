package co.tz.sheriaconnectapi.exceptions;

public class StoryNotFoundException extends DomainException {
    public StoryNotFoundException() {
        super(ErrorMessages.STORY_NOT_FOUND);
    }
}
