package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record IncidentReportReplyInput(
        String caseNumber,
        IncidentReportReplyRequest request,
        String trackingToken,
        Authentication authentication
) {
}
