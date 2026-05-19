package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.CreateStoryModerationNoteInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryModerationNoteRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.StoryModerationNoteResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StorySearchInput;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStoryModerationInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStoryModerationRequest;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.services.StoryServices.AdminListStoriesService;
import co.tz.sheriaconnectapi.services.StoryServices.CreateStoryModerationNoteService;
import co.tz.sheriaconnectapi.services.StoryServices.GetStoryService;
import co.tz.sheriaconnectapi.services.StoryServices.UpdateStoryModerationService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/stories")
public class AdminStoryController {

    private final AdminListStoriesService adminListStoriesService;
    private final GetStoryService getStoryService;
    private final UpdateStoryModerationService updateStoryModerationService;
    private final CreateStoryModerationNoteService createStoryModerationNoteService;

    public AdminStoryController(
            AdminListStoriesService adminListStoriesService,
            GetStoryService getStoryService,
            UpdateStoryModerationService updateStoryModerationService,
            CreateStoryModerationNoteService createStoryModerationNoteService
    ) {
        this.adminListStoriesService = adminListStoriesService;
        this.getStoryService = getStoryService;
        this.updateStoryModerationService = updateStoryModerationService;
        this.createStoryModerationNoteService = createStoryModerationNoteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PUBLICSTORY_READ')")
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) StoryModerationStatus status
    ) {
        return adminListStoriesService.execute(
                new StorySearchInput(category, region, district, status, null)
        );
    }

    @GetMapping("/{publicId}")
    @PreAuthorize("hasAuthority('PUBLICSTORY_READ')")
    public ResponseEntity<StandardResponse<StoryResponse>> get(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return getStoryService.execute(new StoryLookupInput(publicId, authentication, true));
    }

    @PatchMapping("/{publicId}/moderation")
    @PreAuthorize("hasAuthority('PUBLICSTORY_UPDATE')")
    public ResponseEntity<StandardResponse<StoryResponse>> moderate(
            @PathVariable String publicId,
            @RequestBody UpdateStoryModerationRequest request,
            Authentication authentication
    ) {
        return updateStoryModerationService.execute(
                new UpdateStoryModerationInput(publicId, request, authentication)
        );
    }

    @PostMapping("/{publicId}/notes")
    @PreAuthorize("hasAuthority('STORYMODERATIONNOTE_CREATE')")
    public ResponseEntity<StandardResponse<StoryModerationNoteResponse>> addNote(
            @PathVariable String publicId,
            @RequestBody CreateStoryModerationNoteRequest request,
            Authentication authentication
    ) {
        return createStoryModerationNoteService.execute(
                new CreateStoryModerationNoteInput(publicId, request, authentication)
        );
    }
}
