package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record UpdateStoryInput(
        String publicId,
        CreateStoryRequest request,
        Authentication authentication
) {
}
