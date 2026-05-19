package co.tz.sheriaconnectapi.model.DTOs;

import java.time.Instant;

public record StoryContentReportResponse(
        Long id,
        String reason,
        String details,
        String reporterEmail,
        Instant createdAt
) {
}
