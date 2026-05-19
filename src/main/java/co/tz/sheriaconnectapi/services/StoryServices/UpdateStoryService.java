package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryContentException;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedStoryAccessException;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStoryInput;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UpdateStoryService implements Command<UpdateStoryInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public UpdateStoryService(
            PublicStoryRepository publicStoryRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryResponse>> execute(UpdateStoryInput input) {
        CreateStoryRequest request = input.request();
        validate(request);

        User author = storyAccessService.requireAuthenticatedUser(input.authentication());
        PublicStory story = publicStoryRepository.findByPublicId(input.publicId())
                .orElseThrow(StoryNotFoundException::new);

        if (!storyAccessService.isOwner(story, author)) {
            throw new UnauthorizedStoryAccessException();
        }

        if (story.getModerationStatus() != StoryModerationStatus.REJECTED
                && story.getModerationStatus() != StoryModerationStatus.PENDING_REVIEW) {
            throw new InvalidStoryContentException(
                    "Only rejected or pending stories can be edited and resubmitted"
            );
        }

        story.setTitle(request.title().trim());
        story.setBody(request.body().trim());
        story.setCategory(request.category().trim());
        story.setRegion(trimToNull(request.region()));
        story.setDistrict(trimToNull(request.district()));
        story.setAnonymityMode(request.anonymityMode());
        story.setDisplayName(displayNameFor(request));
        story.setModerationStatus(StoryModerationStatus.PENDING_REVIEW);
        story.setRejectionReason(null);
        story.setPublishedAt(null);

        PublicStory savedStory = publicStoryRepository.save(story);

        return ResponseUtil.success(
                storyResponseFactory.detail(savedStory, author, false),
                "Story resubmitted for moderation",
                HttpStatus.OK
        );
    }

    private void validate(CreateStoryRequest request) {
        if (request == null) {
            throw new InvalidStoryContentException("Story payload is required");
        }
        if (isBlank(request.title())) {
            throw new InvalidStoryContentException("Story title is required");
        }
        if (request.title().trim().length() > 180) {
            throw new InvalidStoryContentException("Story title must be 180 characters or less");
        }
        if (isBlank(request.body())) {
            throw new InvalidStoryContentException("Story body is required");
        }
        if (request.body().trim().length() < 40) {
            throw new InvalidStoryContentException("Story body must be at least 40 characters");
        }
        if (isBlank(request.category())) {
            throw new InvalidStoryContentException("Story category is required");
        }
        if (request.anonymityMode() == null) {
            throw new InvalidStoryContentException("Story anonymity mode is required");
        }
        if (request.anonymityMode() == AnonymityMode.NAMED) {
            throw new InvalidStoryContentException("Public stories can be anonymous or pseudonymous only");
        }
        if (request.anonymityMode() == AnonymityMode.PSEUDONYMOUS && isBlank(request.displayName())) {
            throw new InvalidStoryContentException("Display name is required for pseudonymous stories");
        }
    }

    private String displayNameFor(CreateStoryRequest request) {
        if (request.anonymityMode() == AnonymityMode.FULLY_ANONYMOUS) {
            return null;
        }

        return trimToNull(request.displayName());
    }

    private String trimToNull(String value) {
        if (isBlank(value)) {
            return null;
        }

        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
