package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record StoryReactionInput(
        String publicId,
        StoryReactionRequest request,
        Authentication authentication
) {
}
