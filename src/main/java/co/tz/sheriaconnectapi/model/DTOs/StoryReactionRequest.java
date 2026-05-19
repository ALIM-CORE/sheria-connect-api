package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.StoryReactionType;

public record StoryReactionRequest(
        StoryReactionType reactionType
) {
}
