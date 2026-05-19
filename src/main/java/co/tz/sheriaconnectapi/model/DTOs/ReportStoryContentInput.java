package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record ReportStoryContentInput(
        String publicId,
        ReportStoryContentRequest request,
        Authentication authentication
) {
}
