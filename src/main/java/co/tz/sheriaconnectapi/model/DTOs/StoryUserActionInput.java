package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record StoryUserActionInput(
        String publicId,
        Authentication authentication
) {
}
