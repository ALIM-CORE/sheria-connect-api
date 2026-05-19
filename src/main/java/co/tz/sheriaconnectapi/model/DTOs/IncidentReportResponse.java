package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
public class IncidentReportResponse {
    private final Long id;
    private final String caseNumber;
    private final String trackingToken;
    private final Long reporterUserId;
    private final String reporterEmail;
    private final AnonymityMode anonymityMode;
    private final IncidentType incidentType;
    private final IncidentUrgency urgency;
    private final IncidentReportStatus status;
    private final String title;
    private final String description;
    private final LocalDate incidentDate;
    private final String locationDescription;
    private final String region;
    private final String district;
    private final String ward;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String pseudonym;
    private final String contactName;
    private final String contactEmail;
    private final String contactPhone;
    private final boolean matchingRequested;
    private final int evidenceCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<EvidenceFileResponse> evidenceFiles;
    private final List<CaseStatusHistoryResponse> statusHistory;
    private final List<AdminCaseNoteResponse> adminNotes;

    public IncidentReportResponse(
            IncidentReport report,
            String trackingToken,
            List<EvidenceFile> evidenceFiles,
            List<CaseStatusHistory> statusHistory,
            List<AdminCaseNote> adminNotes
    ) {
        this.id = report.getId();
        this.caseNumber = report.getCaseNumber();
        this.trackingToken = trackingToken;
        this.reporterUserId = report.getReporterUser() == null
                ? null
                : report.getReporterUser().getId();
        this.reporterEmail = report.getReporterUser() == null
                ? null
                : report.getReporterUser().getEmail();
        this.anonymityMode = report.getAnonymityMode();
        this.incidentType = report.getIncidentType();
        this.urgency = report.getUrgency();
        this.status = report.getStatus();
        this.title = report.getTitle();
        this.description = report.getDescription();
        this.incidentDate = report.getIncidentDate();
        this.locationDescription = report.getLocationDescription();
        this.region = report.getRegion();
        this.district = report.getDistrict();
        this.ward = report.getWard();
        this.latitude = report.getLatitude();
        this.longitude = report.getLongitude();
        this.pseudonym = report.getPseudonym();
        this.contactName = report.getContactName();
        this.contactEmail = report.getContactEmail();
        this.contactPhone = report.getContactPhone();
        this.matchingRequested = report.isMatchingRequested();
        this.createdAt = report.getCreatedAt();
        this.updatedAt = report.getUpdatedAt();
        this.evidenceFiles = evidenceFiles.stream()
                .map(EvidenceFileResponse::new)
                .toList();
        this.evidenceCount = this.evidenceFiles.size();
        this.statusHistory = statusHistory.stream()
                .map(CaseStatusHistoryResponse::new)
                .toList();
        this.adminNotes = adminNotes.stream()
                .map(AdminCaseNoteResponse::new)
                .toList();
    }
}
