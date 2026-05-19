package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.StoryModerationMetricsResponse;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryContentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetStoryModerationMetricsService implements Query<Void, StoryModerationMetricsResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryContentReportRepository storyContentReportRepository;

    public GetStoryModerationMetricsService(
            PublicStoryRepository publicStoryRepository,
            StoryContentReportRepository storyContentReportRepository
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyContentReportRepository = storyContentReportRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryModerationMetricsResponse>> execute(Void input) {
        StoryModerationMetricsResponse metrics = new StoryModerationMetricsResponse(
                publicStoryRepository.count(),
                publicStoryRepository.countByModerationStatus(StoryModerationStatus.PENDING_REVIEW),
                publicStoryRepository.countByModerationStatus(StoryModerationStatus.PUBLISHED),
                publicStoryRepository.countByModerationStatus(StoryModerationStatus.REJECTED),
                publicStoryRepository.countByModerationStatus(StoryModerationStatus.HIDDEN),
                storyContentReportRepository.countDistinctReportedStories(),
                storyContentReportRepository.count()
        );

        return ResponseUtil.success(metrics, "Story moderation metrics retrieved successfully", HttpStatus.OK);
    }
}
