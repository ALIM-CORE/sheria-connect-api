package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryContentException;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryModerationNoteInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryModerationNoteRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryModerationNoteResponse;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryModerationNote;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryModerationNoteRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CreateStoryModerationNoteService
        implements Command<CreateStoryModerationNoteInput, StoryModerationNoteResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryModerationNoteRepository storyModerationNoteRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public CreateStoryModerationNoteService(
            PublicStoryRepository publicStoryRepository,
            StoryModerationNoteRepository storyModerationNoteRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyModerationNoteRepository = storyModerationNoteRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<StoryModerationNoteResponse>> execute(
            CreateStoryModerationNoteInput input
    ) {
        CreateStoryModerationNoteRequest request = input.request();
        if (request == null || request.note() == null || request.note().isBlank()) {
            throw new InvalidStoryContentException("Moderation note is required");
        }

        User admin = storyAccessService.requireAuthenticatedUser(input.authentication());
        PublicStory story = publicStoryRepository.findByPublicId(input.publicId())
                .orElseThrow(StoryNotFoundException::new);

        StoryModerationNote note = new StoryModerationNote();
        note.setStory(story);
        note.setAdminUser(admin);
        note.setNote(request.note().trim());

        StoryModerationNote savedNote = storyModerationNoteRepository.save(note);

        return ResponseUtil.success(
                storyResponseFactory.moderationNote(savedNote),
                "Story moderation note added",
                HttpStatus.CREATED
        );
    }
}
