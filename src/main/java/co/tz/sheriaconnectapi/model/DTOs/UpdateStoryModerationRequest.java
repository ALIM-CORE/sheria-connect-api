package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;

public record UpdateStoryModerationRequest(
        StoryModerationStatus status,
        String reason
) {
}
