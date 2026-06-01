package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import lombok.Getter;

import java.time.Instant;

@Getter
public class MatchingRequestResponse {
    private final Long id;
    private final String caseNumber;
    private final IncidentReportSummaryResponse report;
    private final ProviderProfileResponse providerProfile;
    private final MatchingRequestStatus status;
    private final int score;
    private final String scoreBreakdown;
    private final Long requestedByUserId;
    private final Long decidedByUserId;
    private final String notes;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final ProviderAcceptedCaseDetailResponse acceptedCaseDetail;

    public MatchingRequestResponse(CaseMatchRequest matchRequest) {
        this(matchRequest, true, null);
    }

    public MatchingRequestResponse(
            CaseMatchRequest matchRequest,
            boolean includeReporterIdentity,
            ProviderAcceptedCaseDetailResponse acceptedCaseDetail
    ) {
        this.id = matchRequest.getId();
        this.caseNumber = matchRequest.getIncidentReport().getCaseNumber();
        this.report = new IncidentReportSummaryResponse(
                matchRequest.getIncidentReport(),
                includeReporterIdentity
        );
        this.providerProfile = new ProviderProfileResponse(matchRequest.getProviderProfile());
        this.status = matchRequest.getStatus();
        this.score = matchRequest.getScore();
        this.scoreBreakdown = matchRequest.getScoreBreakdown();
        this.requestedByUserId = matchRequest.getRequestedBy() == null
                ? null
                : matchRequest.getRequestedBy().getId();
        this.decidedByUserId = matchRequest.getDecidedBy() == null
                ? null
                : matchRequest.getDecidedBy().getId();
        this.notes = matchRequest.getNotes();
        this.createdAt = matchRequest.getCreatedAt();
        this.updatedAt = matchRequest.getUpdatedAt();
        this.acceptedCaseDetail = acceptedCaseDetail;
    }
}
