package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.CaseMessageSenderRole;
import co.tz.sheriaconnectapi.repositories.CaseMessageRepository;
import co.tz.sheriaconnectapi.services.NotificationServices.NotificationDispatchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendCitizenCaseMessageServiceTest {

    @Mock
    private CaseMessageAccessService accessService;
    @Mock
    private CaseMessageRepository caseMessageRepository;
    @Mock
    private NotificationDispatchService notificationDispatchService;

    @Test
    void guestMessageIsStoredWithoutSenderUser() {
        IncidentReport report = new IncidentReport();
        report.setCaseNumber("SC-2609-GUEST03");
        report.setAnonymityMode(AnonymityMode.FULLY_ANONYMOUS);

        User providerUser = new User();
        providerUser.setId(14L);
        ProviderProfile providerProfile = new ProviderProfile();
        providerProfile.setUser(providerUser);
        CaseMatchRequest matchRequest = new CaseMatchRequest();
        matchRequest.setId(27L);
        matchRequest.setIncidentReport(report);
        matchRequest.setProviderProfile(providerProfile);

        when(accessService.requireCitizenContext(
                report.getCaseNumber(),
                null,
                "valid-tracking-token"
        )).thenReturn(new CaseMessageAccessService.CitizenMessageContext(
                null,
                report,
                matchRequest
        ));
        when(caseMessageRepository.save(any(CaseMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var service = new SendCitizenCaseMessageService(
                accessService,
                caseMessageRepository,
                notificationDispatchService
        );
        service.execute(new CaseMessageInput(
                report.getCaseNumber(),
                null,
                null,
                new CaseMessageRequest("  Please help me  "),
                null,
                "valid-tracking-token"
        ));

        ArgumentCaptor<CaseMessage> messageCaptor = ArgumentCaptor.forClass(CaseMessage.class);
        verify(caseMessageRepository).save(messageCaptor.capture());
        CaseMessage saved = messageCaptor.getValue();
        assertNull(saved.getSenderUser());
        assertEquals(CaseMessageSenderRole.CITIZEN, saved.getSenderRole());
        assertEquals("Please help me", saved.getBody());
    }
}
