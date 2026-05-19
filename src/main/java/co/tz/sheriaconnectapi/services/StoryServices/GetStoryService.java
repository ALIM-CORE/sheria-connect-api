package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.StoryNotPublishedException;
import co.tz.sheriaconnectapi.model.DTOs.StoryLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetStoryService implements Query<StoryLookupInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public GetStoryService(
            PublicStoryRepository publicStoryRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryResponse>> execute(StoryLookupInput input) {
        PublicStory story = publicStoryRepository.findByPublicId(input.publicId())
                .orElseThrow(StoryNotFoundException::new);
        User viewer = storyAccessService.authenticatedUser(input.authentication()).orElse(null);

        boolean published = story.getModerationStatus() == StoryModerationStatus.PUBLISHED;
        boolean owner = storyAccessService.isOwner(story, viewer);

        if (!input.admin() && !published && !owner) {
            throw new StoryNotPublishedException();
        }

        return ResponseUtil.success(
                storyResponseFactory.detail(story, viewer, input.admin()),
                "Story retrieved successfully",
                HttpStatus.OK
        );
    }
}
