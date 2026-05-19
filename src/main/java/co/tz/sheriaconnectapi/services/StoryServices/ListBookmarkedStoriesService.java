package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.StoryBookmark;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.StoryBookmarkRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListBookmarkedStoriesService implements Query<Authentication, List<StorySummaryResponse>> {

    private final StoryBookmarkRepository storyBookmarkRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public ListBookmarkedStoriesService(
            StoryBookmarkRepository storyBookmarkRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.storyBookmarkRepository = storyBookmarkRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> execute(Authentication authentication) {
        User user = storyAccessService.requireAuthenticatedUser(authentication);
        List<StorySummaryResponse> stories = storyBookmarkRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(StoryBookmark::getStory)
                .filter(story -> story.getModerationStatus() == StoryModerationStatus.PUBLISHED)
                .map(story -> storyResponseFactory.summary(story, user))
                .toList();

        return ResponseUtil.success(stories, "Bookmarked stories retrieved successfully", HttpStatus.OK);
    }
}
