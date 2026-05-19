package co.tz.sheriaconnectapi.model.DTOs;

import org.springframework.security.core.Authentication;

public record CreateIncidentReportInput(
        CreateIncidentReportRequest request,
        Authentication authentication
) {
}
