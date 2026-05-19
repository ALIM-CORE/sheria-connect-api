package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.StoryNotPublishedException;
import co.tz.sheriaconnectapi.model.DTOs.StoryReactionInput;
import co.tz.sheriaconnectapi.model.DTOs.StoryReactionRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryReaction;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.model.Enums.StoryReactionType;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryReactionRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReactToStoryService implements Command<StoryReactionInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryReactionRepository storyReactionRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public ReactToStoryService(
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
    public ResponseEntity<StandardResponse<StoryResponse>> execute(StoryReactionInput input) {
        PublicStory story = publishedStory(input.publicId());
        User user = storyAccessService.requireAuthenticatedUser(input.authentication());
        StoryReactionType reactionType = reactionType(input.request());

        StoryReaction reaction = storyReactionRepository.findByStoryAndUser(story, user)
                .orElseGet(StoryReaction::new);
        reaction.setStory(story);
        reaction.setUser(user);
        reaction.setReactionType(reactionType);
        storyReactionRepository.save(reaction);

        return ResponseUtil.success(
                storyResponseFactory.detail(story, user, false),
                "Story reaction saved",
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

    private StoryReactionType reactionType(StoryReactionRequest request) {
        if (request == null || request.reactionType() == null) {
            return StoryReactionType.SOLIDARITY;
        }

        return request.reactionType();
    }
}
