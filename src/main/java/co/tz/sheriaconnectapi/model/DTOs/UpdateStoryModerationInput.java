package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record UpdateStoryModerationInput(
        String publicId,
        UpdateStoryModerationRequest request,
        Authentication authentication
) {
}
