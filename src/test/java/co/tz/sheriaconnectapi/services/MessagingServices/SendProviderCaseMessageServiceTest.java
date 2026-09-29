package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.CaseMessageRepository;
import co.tz.sheriaconnectapi.services.NotificationServices.NotificationDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendProviderCaseMessageServiceTest {

    @Mock
    private CaseMessageAccessService accessService;
    @Mock
    private CaseMessageRepository caseMessageRepository;
    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Test
    void providerReplyToGuestReportDoesNotRequireNotificationRecipient() {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-GUEST04");

        CaseMatchRequest matchRequest = new CaseMatchRequest();
        matchRequest.setId(31L);
        matchRequest.setIncidentReport(report);

        User provider = new User();
        provider.setId(19L);
        when(accessService.requireProviderContext(31L, null))
                .thenReturn(new CaseMessageAccessService.ProviderMessageContext(
                        provider,
                        matchRequest
                ));
        when(caseMessageRepository.save(any(CaseMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new SendProviderCaseMessageService(
                accessService,
                caseMessageRepository,
                notificationDispatchService
        );

        assertDoesNotThrow(() -> service.execute(new CaseMessageInput(
                null,
                31L,
                null,
                new CaseMessageRequest("Provider reply"),
                null,
                null
        )));
    }
}
