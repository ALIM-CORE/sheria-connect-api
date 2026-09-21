package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import lombok.Getter;

import java.time.Instant;

@Getter
public class IncidentReportSummaryResponse {
    private final String caseNumber;
    private final Long reporterUserId;
    private final String reporterEmail;
    private final AnonymityMode anonymityMode;
    private final String incidentType;
    private final IncidentUrgency urgency;
    private final IncidentReportStatus status;
    private final String title;
    private final String region;
    private final String district;
    private final boolean matchingRequested;
    private final Instant createdAt;
    private final Instant updatedAt;

    public IncidentReportSummaryResponse(IncidentReport report) {
        this(report, true);
    }

    public IncidentReportSummaryResponse(IncidentReport report, boolean includeReporterIdentity) {
        this.caseNumber = report.getCaseNumber();
        this.reporterUserId = !includeReporterIdentity || report.getReporterUser() == null
                ? null
                : report.getReporterUser().getId();
        this.reporterEmail = !includeReporterIdentity || report.getReporterUser() == null
                ? null
                : report.getReporterUser().getEmail();
        this.anonymityMode = report.getAnonymityMode();
        this.incidentType = report.getIncidentType();
        this.urgency = report.getUrgency();
        this.status = report.getStatus();
        this.title = report.getTitle();
        this.region = report.getRegion();
        this.district = report.getDistrict();
        this.matchingRequested = report.isMatchingRequested();
        this.createdAt = report.getCreatedAt();
        this.updatedAt = report.getUpdatedAt();
    }
}
