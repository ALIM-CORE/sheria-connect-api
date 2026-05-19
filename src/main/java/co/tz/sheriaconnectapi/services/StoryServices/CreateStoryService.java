package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryContentException;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
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
public class CreateStoryService implements Command<CreateStoryInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryAccessService storyAccessService;
    private final PublicStoryIdGeneratorService publicStoryIdGeneratorService;
    private final StoryResponseFactory storyResponseFactory;

    public CreateStoryService(
            PublicStoryRepository publicStoryRepository,
            StoryAccessService storyAccessService,
            PublicStoryIdGeneratorService publicStoryIdGeneratorService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyAccessService = storyAccessService;
        this.publicStoryIdGeneratorService = publicStoryIdGeneratorService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryResponse>> execute(CreateStoryInput input) {
        CreateStoryRequest request = input.request();
        validate(request);

        User author = storyAccessService.requireAuthenticatedUser(input.authentication());

        PublicStory story = new PublicStory();
        story.setPublicId(publicStoryIdGeneratorService.generate());
        story.setAuthorUser(author);
        story.setTitle(request.title().trim());
        story.setBody(request.body().trim());
        story.setCategory(request.category().trim());
        story.setRegion(trimToNull(request.region()));
        story.setDistrict(trimToNull(request.district()));
        story.setAnonymityMode(request.anonymityMode());
        story.setDisplayName(displayNameFor(request));
        story.setModerationStatus(StoryModerationStatus.PENDING_REVIEW);

        PublicStory savedStory = publicStoryRepository.save(story);

        return ResponseUtil.success(
                storyResponseFactory.detail(savedStory, author, false),
                "Story submitted for moderation",
                HttpStatus.CREATED
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
