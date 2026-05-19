package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CreateStoryModerationNoteInput(
        String publicId,
        CreateStoryModerationNoteRequest request,
        Authentication authentication
) {
}
