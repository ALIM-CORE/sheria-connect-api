package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseStatusHistory;
import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
public class ProviderAcceptedCaseDetailResponse {
    private final String caseNumber;
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
    private final boolean matchingRequested;
    private final int evidenceCount;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<EvidenceFileResponse> evidenceFiles;
    private final List<CaseStatusHistoryResponse> statusHistory;

    public ProviderAcceptedCaseDetailResponse(
            IncidentReport report,
            List<EvidenceFile> evidenceFiles,
            List<CaseStatusHistory> statusHistory
    ) {
        this.caseNumber = report.getCaseNumber();
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
    }
}
