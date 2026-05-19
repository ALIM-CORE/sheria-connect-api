package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.StoryNotPublishedException;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryUserActionInput;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryReactionRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveStoryReactionService implements Command<StoryUserActionInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryReactionRepository storyReactionRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public RemoveStoryReactionService(
            PublicStoryRepository publicStoryRepository,
            StoryReactionRepository storyReactionRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyReactionRepository = storyReactionRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<StoryResponse>> execute(StoryUserActionInput input) {
        PublicStory story = publishedStory(input.publicId());
        User user = storyAccessService.requireAuthenticatedUser(input.authentication());
        storyReactionRepository.deleteByStoryAndUser(story, user);

        return ResponseUtil.success(
                storyResponseFactory.detail(story, user, false),
                "Story reaction removed",
                HttpStatus.OK
        );
    }

    private PublicStory publishedStory(String publicId) {
        PublicStory story = publicStoryRepository.findByPublicId(publicId)
                .orElseThrow(StoryNotFoundException::new);
        if (story.getModerationStatus() != StoryModerationStatus.PUBLISHED) {
            throw new StoryNotPublishedException();
        }
        return story;
    }
}
