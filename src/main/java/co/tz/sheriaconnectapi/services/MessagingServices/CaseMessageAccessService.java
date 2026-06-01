package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.exceptions.CaseMessageAccessDeniedException;
import co.tz.sheriaconnectapi.exceptions.IncidentReportNotFoundException;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.services.IncidentReportServices.IncidentReportAccessService;
import co.tz.sheriaconnectapi.services.MatchingServices.ProviderCaseRequestAccessService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CaseMessageAccessService {

    private final IncidentReportRepository incidentReportRepository;
    private final CaseMatchRequestRepository caseMatchRequestRepository;
    private final IncidentReportAccessService incidentReportAccessService;
    private final ProviderCaseRequestAccessService providerCaseRequestAccessService;

    public CaseMessageAccessService(
            IncidentReportRepository incidentReportRepository,
            CaseMatchRequestRepository caseMatchRequestRepository,
            IncidentReportAccessService incidentReportAccessService,
            ProviderCaseRequestAccessService providerCaseRequestAccessService
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
        this.incidentReportAccessService = incidentReportAccessService;
        this.providerCaseRequestAccessService = providerCaseRequestAccessService;
    }

    public CitizenMessageContext requireCitizenContext(String caseNumber, Authentication authentication) {
        User user = incidentReportAccessService.requireAuthenticatedUser(authentication);
        IncidentReport report = incidentReportRepository.findByCaseNumber(caseNumber)
                .orElseThrow(IncidentReportNotFoundException::new);

        if (report.getReporterUser() == null || !report.getReporterUser().getId().equals(user.getId())) {
            throw new CaseMessageAccessDeniedException();
        }

        List<CaseMatchRequest> accepted = caseMatchRequestRepository
                .findByIncidentReportAndStatusOrderByCreatedAtAsc(
                        report,
                        MatchingRequestStatus.ACCEPTED
                );
        if (accepted.isEmpty()) {
            throw new CaseMessageAccessDeniedException();
        }

        return new CitizenMessageContext(user, report, accepted.getFirst());
    }

    public ProviderMessageContext requireProviderContext(Long matchingRequestId, Authentication authentication) {
        User user = incidentReportAccessService.requireAuthenticatedUser(authentication);
        CaseMatchRequest request = providerCaseRequestAccessService
                .requireMyRequest(matchingRequestId, authentication);
        if (request.getStatus() != MatchingRequestStatus.ACCEPTED) {
            throw new CaseMessageAccessDeniedException();
        }

        return new ProviderMessageContext(user, request);
    }

    public record CitizenMessageContext(
            User user,
            IncidentReport report,
            CaseMatchRequest matchRequest
    ) {
    }

    public record ProviderMessageContext(
            User user,
            CaseMatchRequest matchRequest
    ) {
    }
}
