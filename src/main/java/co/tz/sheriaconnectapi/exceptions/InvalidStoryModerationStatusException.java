package co.tz.sheriaconnectapi.exceptions;

public class InvalidStoryModerationStatusException extends RuntimeException {
    public InvalidStoryModerationStatusException() {
        super(ErrorMessages.INVALID_STORY_MODERATION_STATUS.getMessage());
    }
}
