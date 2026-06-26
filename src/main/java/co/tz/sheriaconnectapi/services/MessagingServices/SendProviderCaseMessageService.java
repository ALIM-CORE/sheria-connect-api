package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageResponse;
import co.tz.sheriaconnectapi.model.Entities.CaseMessage;
import co.tz.sheriaconnectapi.model.Enums.CaseMessageSenderRole;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.CaseMessageRepository;
import co.tz.sheriaconnectapi.services.NotificationServices.NotificationDispatchService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class SendProviderCaseMessageService implements Command<CaseMessageInput, CaseMessageResponse> {

    private final CaseMessageAccessService accessService;
    private final CaseMessageRepository caseMessageRepository;
    private final NotificationDispatchService notificationDispatchService;

    public SendProviderCaseMessageService(
            CaseMessageAccessService accessService,
            CaseMessageRepository caseMessageRepository,
            NotificationDispatchService notificationDispatchService
    ) {
        this.accessService = accessService;
        this.caseMessageRepository = caseMessageRepository;
        this.notificationDispatchService = notificationDispatchService;
    }

    @Override
    public ResponseEntity<StandardResponse<CaseMessageResponse>> execute(CaseMessageInput input) {
        if (input.request() == null || input.request().body() == null || input.request().body().isBlank()) {
            throw new UserNotValidException("Message body is required");
        }

        var context = accessService.requireProviderContext(
                input.matchingRequestId(),
                input.authentication()
        );
        CaseMessage message = new CaseMessage();
        message.setIncidentReport(context.matchRequest().getIncidentReport());
        message.setCaseMatchRequest(context.matchRequest());
        message.setSenderUser(context.user());
        message.setSenderRole(CaseMessageSenderRole.PROVIDER);
        message.setBody(input.request().body().trim());
        CaseMessage saved = caseMessageRepository.save(message);

        notificationDispatchService.notify(
                context.matchRequest().getIncidentReport().getReporterUser(),
                AccessContext.CITIZEN,
                NotificationType.CASE_MESSAGE_CREATED,
                "New provider message",
                "A provider sent a message on case "
                        + context.matchRequest().getIncidentReport().getCaseNumber()
                        + ".",
                "INCIDENT_REPORT",
                context.matchRequest().getIncidentReport().getCaseNumber()
        );

        return ResponseUtil.success(
                new CaseMessageResponse(saved),
                "Message sent",
                HttpStatus.CREATED
        );
    }
}
