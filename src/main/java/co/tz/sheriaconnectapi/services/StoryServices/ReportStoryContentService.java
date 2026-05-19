package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidStoryContentException;
import co.tz.sheriaconnectapi.exceptions.StoryNotFoundException;
import co.tz.sheriaconnectapi.exceptions.StoryNotPublishedException;
import co.tz.sheriaconnectapi.model.DTOs.ReportStoryContentInput;
import co.tz.sheriaconnectapi.model.DTOs.ReportStoryContentRequest;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryContentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import co.tz.sheriaconnectapi.repositories.PublicStoryRepository;
import co.tz.sheriaconnectapi.repositories.StoryContentReportRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class ReportStoryContentService implements Command<ReportStoryContentInput, Void> {

    private final PublicStoryRepository publicStoryRepository;
    private final StoryContentReportRepository storyContentReportRepository;
    private final StoryAccessService storyAccessService;

    public ReportStoryContentService(
            PublicStoryRepository publicStoryRepository,
            StoryContentReportRepository storyContentReportRepository,
            StoryAccessService storyAccessService
    ) {
        this.publicStoryRepository = publicStoryRepository;
        this.storyContentReportRepository = storyContentReportRepository;
        this.storyAccessService = storyAccessService;
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> execute(ReportStoryContentInput input) {
        ReportStoryContentRequest request = input.request();
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new InvalidStoryContentException("Content report reason is required");
        }

        PublicStory story = publicStoryRepository.findByPublicId(input.publicId())
                .orElseThrow(StoryNotFoundException::new);
        if (story.getModerationStatus() != StoryModerationStatus.PUBLISHED) {
            throw new StoryNotPublishedException();
        }

        User reporter = storyAccessService.authenticatedUser(input.authentication()).orElse(null);

        StoryContentReport contentReport = new StoryContentReport();
        contentReport.setStory(story);
        contentReport.setReporterUser(reporter);
        contentReport.setReason(request.reason().trim());
        contentReport.setDetails(trimToNull(request.details()));
        storyContentReportRepository.save(contentReport);

        return ResponseUtil.success(null, "Story content report submitted", HttpStatus.CREATED);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
