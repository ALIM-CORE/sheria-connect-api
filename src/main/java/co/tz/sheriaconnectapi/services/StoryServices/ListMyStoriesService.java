package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMyStoriesService implements Query<Authentication, List<StorySummaryResponse>> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public ListMyStoriesService(
            PublicStoryRepository publicStoryRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> execute(Authentication authentication) {
        User user = storyAccessService.requireAuthenticatedUser(authentication);
        List<StorySummaryResponse> stories = publicStoryRepository
                .findByAuthorUserOrderByCreatedAtDesc(user)
                .stream()
                .map(story -> storyResponseFactory.summary(story, user))
                .toList();

        return ResponseUtil.success(stories, "Your stories retrieved successfully", HttpStatus.OK);
    }
}
