package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import org.springframework.security.core.Authentication;

public record StorySearchInput(
        String category,
        String region,
        String district,
        StoryModerationStatus status,
        Boolean reportedOnly,
        Authentication authentication
) {
}
