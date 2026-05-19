package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.CreateStoryInput;
import co.tz.sheriaconnectapi.model.DTOs.CreateStoryRequest;
import co.tz.sheriaconnectapi.model.DTOs.ReportStoryContentInput;
import co.tz.sheriaconnectapi.model.DTOs.ReportStoryContentRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryLookupInput;
import co.tz.sheriaconnectapi.model.DTOs.StoryReactionInput;
import co.tz.sheriaconnectapi.model.DTOs.StoryReactionRequest;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StorySearchInput;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryUserActionInput;
import co.tz.sheriaconnectapi.services.StoryServices.BookmarkStoryService;
import co.tz.sheriaconnectapi.services.StoryServices.CreateStoryService;
import co.tz.sheriaconnectapi.services.StoryServices.GetStoryService;
import co.tz.sheriaconnectapi.services.StoryServices.ListMyStoriesService;
import co.tz.sheriaconnectapi.services.StoryServices.ListPublishedStoriesService;
import co.tz.sheriaconnectapi.services.StoryServices.ReactToStoryService;
import co.tz.sheriaconnectapi.services.StoryServices.RemoveStoryBookmarkService;
import co.tz.sheriaconnectapi.services.StoryServices.RemoveStoryReactionService;
import co.tz.sheriaconnectapi.services.StoryServices.ReportStoryContentService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stories")
public class StoryController {

    private final CreateStoryService createStoryService;
    private final ListPublishedStoriesService listPublishedStoriesService;
    private final GetStoryService getStoryService;
    private final ListMyStoriesService listMyStoriesService;
    private final ReactToStoryService reactToStoryService;
    private final RemoveStoryReactionService removeStoryReactionService;
    private final BookmarkStoryService bookmarkStoryService;
    private final RemoveStoryBookmarkService removeStoryBookmarkService;
    private final ReportStoryContentService reportStoryContentService;

    public StoryController(
            CreateStoryService createStoryService,
            ListPublishedStoriesService listPublishedStoriesService,
            GetStoryService getStoryService,
            ListMyStoriesService listMyStoriesService,
            ReactToStoryService reactToStoryService,
            RemoveStoryReactionService removeStoryReactionService,
            BookmarkStoryService bookmarkStoryService,
            RemoveStoryBookmarkService removeStoryBookmarkService,
            ReportStoryContentService reportStoryContentService
    ) {
        this.createStoryService = createStoryService;
        this.listPublishedStoriesService = listPublishedStoriesService;
        this.getStoryService = getStoryService;
        this.listMyStoriesService = listMyStoriesService;
        this.reactToStoryService = reactToStoryService;
        this.removeStoryReactionService = removeStoryReactionService;
        this.bookmarkStoryService = bookmarkStoryService;
        this.removeStoryBookmarkService = removeStoryBookmarkService;
        this.reportStoryContentService = reportStoryContentService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String district,
            Authentication authentication
    ) {
        return listPublishedStoriesService.execute(
                new StorySearchInput(category, region, district, null, authentication)
        );
    }

    @PostMapping
    public ResponseEntity<StandardResponse<StoryResponse>> create(
            @RequestBody CreateStoryRequest request,
            Authentication authentication
    ) {
        return createStoryService.execute(new CreateStoryInput(request, authentication));
    }

    @GetMapping("/mine")
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> mine(
            Authentication authentication
    ) {
        return listMyStoriesService.execute(authentication);
    }

    @GetMapping("/{publicId}")
    public ResponseEntity<StandardResponse<StoryResponse>> get(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return getStoryService.execute(new StoryLookupInput(publicId, authentication, false));
    }

    @PostMapping("/{publicId}/reactions")
    public ResponseEntity<StandardResponse<StoryResponse>> react(
            @PathVariable String publicId,
            @RequestBody(required = false) StoryReactionRequest request,
            Authentication authentication
    ) {
        return reactToStoryService.execute(
                new StoryReactionInput(publicId, request, authentication)
        );
    }

    @DeleteMapping("/{publicId}/reactions")
    public ResponseEntity<StandardResponse<StoryResponse>> removeReaction(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return removeStoryReactionService.execute(
                new StoryUserActionInput(publicId, authentication)
        );
    }

    @PostMapping("/{publicId}/bookmark")
    public ResponseEntity<StandardResponse<StoryResponse>> bookmark(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return bookmarkStoryService.execute(new StoryUserActionInput(publicId, authentication));
    }

    @DeleteMapping("/{publicId}/bookmark")
    public ResponseEntity<StandardResponse<StoryResponse>> removeBookmark(
            @PathVariable String publicId,
            Authentication authentication
    ) {
        return removeStoryBookmarkService.execute(
                new StoryUserActionInput(publicId, authentication)
        );
    }

    @PostMapping("/{publicId}/reports")
    public ResponseEntity<StandardResponse<Void>> reportContent(
            @PathVariable String publicId,
            @RequestBody ReportStoryContentRequest request,
            Authentication authentication
    ) {
        return reportStoryContentService.execute(
                new ReportStoryContentInput(publicId, request, authentication)
        );
    }
}
