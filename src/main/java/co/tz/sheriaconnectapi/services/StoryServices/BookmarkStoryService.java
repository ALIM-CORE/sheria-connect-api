package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.StoryNotPublishedException;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryUserActionInput;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryBookmark;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryBookmarkRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookmarkStoryService implements Command<StoryUserActionInput, StoryResponse> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryBookmarkRepository storyBookmarkRepository;
    private final StoryAccessService storyAccessService;
    private final StoryResponseFactory storyResponseFactory;

    public BookmarkStoryService(
            PublicStoryRepository publicStoryRepository,
            StoryBookmarkRepository storyBookmarkRepository,
            StoryAccessService storyAccessService,
            StoryResponseFactory storyResponseFactory
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyBookmarkRepository = storyBookmarkRepository;
        this.storyAccessService = storyAccessService;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    @Transactional
    public ResponseEntity<StandardResponse<StoryResponse>> execute(StoryUserActionInput input) {
        PublicStory story = publishedStory(input.publicId());
        User user = storyAccessService.requireAuthenticatedUser(input.authentication());

        storyBookmarkRepository.findByStoryAndUser(story, user)
                .orElseGet(() -> {
                    StoryBookmark bookmark = new StoryBookmark();
                    bookmark.setStory(story);
                    bookmark.setUser(user);
                    return storyBookmarkRepository.save(bookmark);
                });

        return ResponseUtil.success(
                storyResponseFactory.detail(story, user, false),
                "Story bookmarked",
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
