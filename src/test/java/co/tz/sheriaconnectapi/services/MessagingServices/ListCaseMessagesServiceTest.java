package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.repositories.CaseMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCaseMessagesServiceTest {

    @Mock
    private CaseMessageAccessService accessService;
    @Mock
    private CaseMessageRepository caseMessageRepository;
    @Mock
    private Authentication authentication;

    @Test
    void citizenListUsesAfterIdCursorWhenProvided() {
        IncidentReport report = new IncidentReport();
        var context = new CaseMessageAccessService.CitizenMessageContext(null, report, null);
        when(accessService.requireCitizenContext("SC-2608-ABC123", authentication))
                .thenReturn(context);
        when(caseMessageRepository.findByIncidentReportAndIdGreaterThanOrderByCreatedAtAsc(report, 17L))
                .thenReturn(List.of());

        var service = new ListCitizenCaseMessagesService(accessService, caseMessageRepository);
        service.execute(new CaseMessageInput("SC-2608-ABC123", null, 17L, null, authentication));

        verify(caseMessageRepository)
                .findByIncidentReportAndIdGreaterThanOrderByCreatedAtAsc(report, 17L);
    }

    @Test
    void providerListUsesAfterIdCursorWhenProvided() {
        CaseMatchRequest request = new CaseMatchRequest();
        var context = new CaseMessageAccessService.ProviderMessageContext(null, request);
        when(accessService.requireProviderContext(42L, authentication))
                .thenReturn(context);
        when(caseMessageRepository.findByCaseMatchRequestAndIdGreaterThanOrderByCreatedAtAsc(request, 19L))
                .thenReturn(List.of());

        var service = new ListProviderCaseMessagesService(accessService, caseMessageRepository);
        service.execute(new CaseMessageInput(null, 42L, 19L, null, authentication));

        verify(caseMessageRepository)
                .findByCaseMatchRequestAndIdGreaterThanOrderByCreatedAtAsc(request, 19L);
    }
}
