package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderAcceptedCaseDetailResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.CaseStatusHistoryRepository;
import co.tz.sheriaconnectapi.repositories.EvidenceFileRepository;
import org.springframework.stereotype.Component;

@Component
public class ProviderMatchingRequestResponseFactory {

    private final EvidenceFileRepository evidenceFileRepository;
    private final CaseStatusHistoryRepository caseStatusHistoryRepository;

    public ProviderMatchingRequestResponseFactory(
            EvidenceFileRepository evidenceFileRepository,
            CaseStatusHistoryRepository caseStatusHistoryRepository
    ) {
        this.evidenceFileRepository = evidenceFileRepository;
        this.caseStatusHistoryRepository = caseStatusHistoryRepository;
    }

    public MatchingRequestResponse from(CaseMatchRequest matchRequest) {
        ProviderAcceptedCaseDetailResponse detail = null;
        if (matchRequest.getStatus() == MatchingRequestStatus.ACCEPTED) {
            IncidentReport report = matchRequest.getIncidentReport();
            detail = new ProviderAcceptedCaseDetailResponse(
                    report,
                    evidenceFileRepository.findByIncidentReportOrderByCreatedAtDesc(report),
                    caseStatusHistoryRepository.findByIncidentReportOrderByCreatedAtAsc(report)
            );
        }

        return new MatchingRequestResponse(matchRequest, false, detail);
    }
}
