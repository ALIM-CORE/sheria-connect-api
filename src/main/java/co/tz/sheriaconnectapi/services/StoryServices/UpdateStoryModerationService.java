package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryContentException;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryModerationStatusException;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStoryModerationInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStoryModerationRequest;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateStoryModerationService implements Command<UpdateStoryModerationInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public UpdateStoryModerationService(
            PublicStoryRepository publicStoryRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryResponse>> execute(UpdateStoryModerationInput input) {
        UpdateStoryModerationRequest request = input.request();
        if (request == null || request.status() == null) {
            throw new InvalidStoryModerationStatusException();
        }
        if (request.status() == StoryModerationStatus.REJECTED
                && (request.reason() == null || request.reason().isBlank())) {
            throw new InvalidStoryContentException("Rejection reason is required");
        }

        User admin = storyAccessService.requireAuthenticatedUser(input.authentication());
        PublicStory story = publicStoryRepository.findByPublicId(input.publicId())
                .orElseThrow(StoryNotFoundException::new);

        story.setModerationStatus(request.status());
        story.setRejectionReason(reasonFor(request));
        if (request.status() == StoryModerationStatus.PUBLISHED && story.getPublishedAt() == null) {
            story.setPublishedAt(Instant.now());
        }
        if (request.status() != StoryModerationStatus.PUBLISHED) {
            story.setPublishedAt(null);
        }

        PublicStory savedStory = publicStoryRepository.save(story);

        return ResponseUtil.success(
                storyResponseFactory.detail(savedStory, admin, true),
                "Story moderation status updated",
                HttpStatus.OK
        );
    }

    private String reasonFor(UpdateStoryModerationRequest request) {
        if (request.status() == StoryModerationStatus.PUBLISHED
                || request.reason() == null
                || request.reason().isBlank()) {
            return null;
        }

        return request.reason().trim();
    }
}
