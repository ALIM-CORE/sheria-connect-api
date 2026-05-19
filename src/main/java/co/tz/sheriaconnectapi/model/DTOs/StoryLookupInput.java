package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record StoryLookupInput(
        String publicId,
        Authentication authentication,
        boolean admin
) {
}
