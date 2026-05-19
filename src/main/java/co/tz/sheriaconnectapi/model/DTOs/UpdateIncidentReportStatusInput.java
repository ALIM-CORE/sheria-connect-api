package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record UpdateIncidentReportStatusInput(
        String caseNumber,
        UpdateIncidentReportStatusRequest request,
        Authentication authentication
) {
}
