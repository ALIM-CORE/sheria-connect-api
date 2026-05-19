package co.tz.sheriaconnectapi.model.DTOs;

import java.time.Instant;

public record StoryModerationNoteResponse(
        Long id,
        String note,
        String adminName,
        Instant createdAt
) {
}
