package co.tz.sheriaconnectapi.model.DTOs;

public record StoryModerationMetricsResponse(
        long total,
        long pending,
        long published,
        long rejected,
        long hidden,
        long reportedStories,
        long contentReports
) {
}
