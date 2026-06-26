package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        var service = new CaseMessageAccessService(
                incidentReportRepository,
                caseMatchRequestRepository,
                incidentReportAccessService,
                providerCaseRequestAccessService,
                authenticatedUserResolver
        );

        var context = service.requireProviderContext(17L, authentication);

        assertEquals(provider, context.user());
        assertEquals(request, context.matchRequest());
    }
}
