package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;

import java.time.Instant;
import java.util.List;

public record StoryResponse(
        String publicId,
        String title,
        String body,
        String category,
        String region,
        String district,
        AnonymityMode anonymityMode,
        String displayName,
        StoryModerationStatus moderationStatus,
        String rejectionReason,
        long reactionCount,
        boolean bookmarked,
        Instant createdAt,
        Instant publishedAt,
        List<StoryModerationNoteResponse> moderationNotes
) {
}
