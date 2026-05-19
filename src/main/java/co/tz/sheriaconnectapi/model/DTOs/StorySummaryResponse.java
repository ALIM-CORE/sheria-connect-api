package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;

import java.time.Instant;

public record StorySummaryResponse(
        String publicId,
        String title,
        String excerpt,
        String category,
        String region,
        String district,
        AnonymityMode anonymityMode,
        String displayName,
        StoryModerationStatus moderationStatus,
        long reactionCount,
        boolean bookmarked,
        Instant createdAt,
        Instant publishedAt
) {
}
