package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.model.DTOs.StoryContentReportResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryModerationNoteResponse;
import co.tz.sheriaconnectapi.model.DTOs.StoryResponse;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryContentReport;
import co.tz.sheriaconnectapi.model.Entities.StoryModerationNote;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.StoryBookmarkRepository;
import co.tz.sheriaconnectapi.repositories.StoryContentReportRepository;
import co.tz.sheriaconnectapi.repositories.StoryModerationNoteRepository;
import co.tz.sheriaconnectapi.repositories.StoryReactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoryResponseFactory {

    private final StoryReactionRepository storyReactionRepository;
    private final StoryBookmarkRepository storyBookmarkRepository;
    private final StoryModerationNoteRepository storyModerationNoteRepository;
    private final StoryContentReportRepository storyContentReportRepository;

    public StoryResponseFactory(
            StoryReactionRepository storyReactionRepository,
            StoryBookmarkRepository storyBookmarkRepository,
            StoryModerationNoteRepository storyModerationNoteRepository,
            StoryContentReportRepository storyContentReportRepository
    ) {
        this.storyReactionRepository = storyReactionRepository;
        this.storyBookmarkRepository = storyBookmarkRepository;
        this.storyModerationNoteRepository = storyModerationNoteRepository;
        this.storyContentReportRepository = storyContentReportRepository;
    }

    public StorySummaryResponse summary(PublicStory story, User viewer) {
        return summary(story, viewer, false);
    }

    public StorySummaryResponse adminSummary(PublicStory story) {
        return summary(story, null, true);
    }

    private StorySummaryResponse summary(PublicStory story, User viewer, boolean includeContentReportCount) {
        return new StorySummaryResponse(
                story.getPublicId(),
                story.getTitle(),
                excerpt(story.getBody()),
                story.getCategory(),
                story.getRegion(),
                story.getDistrict(),
                story.getAnonymityMode(),
                story.getDisplayName(),
                story.getModerationStatus(),
                story.getRejectionReason(),
                storyReactionRepository.countByStory(story),
                includeContentReportCount ? storyContentReportRepository.countByStory(story) : 0,
                isBookmarked(story, viewer),
                story.getCreatedAt(),
                story.getPublishedAt()
        );
    }

    public StoryResponse detail(PublicStory story, User viewer, boolean includeModerationNotes) {
        List<StoryModerationNoteResponse> moderationNotes = includeModerationNotes
                ? storyModerationNoteRepository.findByStoryOrderByCreatedAtDesc(story)
                        .stream()
                        .map(this::moderationNote)
                        .toList()
                : List.of();
        List<StoryContentReportResponse> contentReports = includeModerationNotes
                ? storyContentReportRepository.findByStoryOrderByCreatedAtDesc(story)
                        .stream()
                        .map(this::contentReport)
                        .toList()
                : List.of();

        return new StoryResponse(
                story.getPublicId(),
                story.getTitle(),
                story.getBody(),
                story.getCategory(),
                story.getRegion(),
                story.getDistrict(),
                story.getAnonymityMode(),
                story.getDisplayName(),
                story.getModerationStatus(),
                story.getRejectionReason(),
                storyReactionRepository.countByStory(story),
                includeModerationNotes ? storyContentReportRepository.countByStory(story) : 0,
                isBookmarked(story, viewer),
                story.getCreatedAt(),
                story.getPublishedAt(),
                moderationNotes,
                contentReports
        );
    }

    public StoryModerationNoteResponse moderationNote(StoryModerationNote note) {
        return new StoryModerationNoteResponse(
                note.getId(),
                note.getNote(),
                note.getAdminUser() == null ? null : note.getAdminUser().getName(),
                note.getCreatedAt()
        );
    }

    public StoryContentReportResponse contentReport(StoryContentReport report) {
        return new StoryContentReportResponse(
                report.getId(),
                report.getReason(),
                report.getDetails(),
                report.getReporterUser() == null ? null : report.getReporterUser().getEmail(),
                report.getCreatedAt()
        );
    }

    private boolean isBookmarked(PublicStory story, User viewer) {
        return viewer != null && storyBookmarkRepository.existsByStoryAndUser(story, viewer);
    }

    private String excerpt(String body) {
        if (body == null) {
            return null;
        }

        String normalized = body.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 180) {
            return normalized;
        }

        return normalized.substring(0, 177) + "...";
    }
}
