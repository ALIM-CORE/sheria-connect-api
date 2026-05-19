package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CreateStoryInput(
        CreateStoryRequest request,
        Authentication authentication
) {
}
