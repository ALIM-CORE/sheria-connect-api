package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.IncidentReportRepository;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import co.tz.sheriaconnectapi.services.IncidentReportServices.IncidentReportAccessService;
import co.tz.sheriaconnectapi.services.MatchingServices.ProviderCaseRequestAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseMessageAccessServiceTest {

    @Mock
    private IncidentReportRepository incidentReportRepository;
    @Mock
    private CaseMatchRequestRepository caseMatchRequestRepository;
    @Mock
    private IncidentReportAccessService incidentReportAccessService;
    @Mock
    private ProviderCaseRequestAccessService providerCaseRequestAccessService;
    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private Authentication authentication;

    @Test
    void guestWithTrackingTokenCanAccessAcceptedCaseMessages() {
        IncidentReport report = report("SC-2609-GUEST01");
        CaseMatchRequest request = acceptedRequest(report);
        when(incidentReportRepository.findByCaseNumber(report.getCaseNumber()))
                .thenReturn(Optional.of(report));
        when(authenticatedUserResolver.authenticatedUser(null)).thenReturn(Optional.empty());
        when(caseMatchRequestRepository.findByIncidentReportAndStatusOrderByCreatedAtAsc(
                report,
                MatchingRequestStatus.ACCEPTED
        )).thenReturn(List.of(request));

        var context = service().requireCitizenContext(
                report.getCaseNumber(),
                null,
                "valid-tracking-token"
        );

        assertNull(context.user());
        assertEquals(report, context.report());
        assertEquals(request, context.matchRequest());
        verify(incidentReportAccessService).assertCitizenAccess(
                report,
                null,
                "valid-tracking-token"
        );
    }

    @Test
    void guestCannotMessageBeforeProviderAcceptsRequest() {
        IncidentReport report = report("SC-2609-GUEST02");
        when(incidentReportRepository.findByCaseNumber(report.getCaseNumber()))
                .thenReturn(Optional.of(report));
        when(authenticatedUserResolver.authenticatedUser(null)).thenReturn(Optional.empty());
        when(caseMatchRequestRepository.findByIncidentReportAndStatusOrderByCreatedAtAsc(
                report,
                MatchingRequestStatus.ACCEPTED
        )).thenReturn(List.of());

        assertThrows(
                co.tz.sheriaconnectapi.exceptions.CaseMessageAccessDeniedException.class,
                () -> service().requireCitizenContext(
                        report.getCaseNumber(),
                        null,
                        "valid-tracking-token"
                )
        );
    }

    @Test
    void providerMessagingUsesProviderContextIdentity() {
        User provider = new User();
        provider.setId(8L);
        CaseMatchRequest request = new CaseMatchRequest();
        request.setId(17L);
        request.setStatus(MatchingRequestStatus.ACCEPTED);

        when(authenticatedUserResolver.requireProviderUser(authentication))
                .thenReturn(provider);
        when(providerCaseRequestAccessService.requireMyRequest(17L, authentication))
                .thenReturn(request);

        var service = service();

        var context = service.requireProviderContext(17L, authentication);

        assertEquals(provider, context.user());
        assertEquals(request, context.matchRequest());
    }

    private CaseMessageAccessService service() {
        return new CaseMessageAccessService(
                incidentReportRepository,
                caseMatchRequestRepository,
                incidentReportAccessService,
                providerCaseRequestAccessService,
                authenticatedUserResolver
        );
    }

    private IncidentReport report(String caseNumber) {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber(caseNumber);
        return report;
    }

    private CaseMatchRequest acceptedRequest(IncidentReport report) {
        CaseMatchRequest request = new CaseMatchRequest();
        request.setId(21L);
        request.setIncidentReport(report);
        request.setStatus(MatchingRequestStatus.ACCEPTED);
        return request;
    }
}
