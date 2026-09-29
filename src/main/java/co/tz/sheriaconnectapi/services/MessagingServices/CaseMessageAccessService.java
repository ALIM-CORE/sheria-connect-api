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
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CaseMessageAccessService {

    private final IncidentReportRepository incidentReportRepository;
    private final CaseMatchRequestRepository caseMatchRequestRepository;
    private final IncidentReportAccessService incidentReportAccessService;
    private final ProviderCaseRequestAccessService providerCaseRequestAccessService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public CaseMessageAccessService(
            IncidentReportRepository incidentReportRepository,
            CaseMatchRequestRepository caseMatchRequestRepository,
            IncidentReportAccessService incidentReportAccessService,
            ProviderCaseRequestAccessService providerCaseRequestAccessService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
        this.incidentReportAccessService = incidentReportAccessService;
        this.providerCaseRequestAccessService = providerCaseRequestAccessService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    public CitizenMessageContext requireCitizenContext(
            String caseNumber,
            Authentication authentication,
            String trackingToken
    ) {
        IncidentReport report = incidentReportRepository.findByCaseNumber(caseNumber)
                .orElseThrow(IncidentReportNotFoundException::new);

        incidentReportAccessService.assertCitizenAccess(report, authentication, trackingToken);
        Optional<User> user = authenticatedUserResolver.authenticatedUser(authentication);

        List<CaseMatchRequest> accepted = caseMatchRequestRepository
                .findByIncidentReportAndStatusOrderByCreatedAtAsc(
                        report,
                        MatchingRequestStatus.ACCEPTED
                );
        if (accepted.isEmpty()) {
            throw new CaseMessageAccessDeniedException();
        }

        return new CitizenMessageContext(user.orElse(null), report, accepted.getFirst());
    }

    public ProviderMessageContext requireProviderContext(Long matchingRequestId, Authentication authentication) {
        User user = authenticatedUserResolver.requireProviderUser(authentication);
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
