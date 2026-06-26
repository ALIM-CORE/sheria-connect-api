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
public class SendCitizenCaseMessageService implements Command<CaseMessageInput, CaseMessageResponse> {

    private final CaseMessageAccessService accessService;
    private final CaseMessageRepository caseMessageRepository;
    private final NotificationDispatchService notificationDispatchService;

    public SendCitizenCaseMessageService(
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

        var context = accessService.requireCitizenContext(input.caseNumber(), input.authentication());
        CaseMessage message = new CaseMessage();
        message.setIncidentReport(context.report());
        message.setCaseMatchRequest(context.matchRequest());
        message.setSenderUser(context.user());
        message.setSenderRole(CaseMessageSenderRole.CITIZEN);
        message.setBody(input.request().body().trim());
        CaseMessage saved = caseMessageRepository.save(message);

        notificationDispatchService.notify(
                context.matchRequest().getProviderProfile().getUser(),
                AccessContext.PROVIDER,
                NotificationType.CASE_MESSAGE_CREATED,
                "New case message",
                "A citizen sent a message on case " + context.report().getCaseNumber() + ".",
                "CASE_REQUEST",
                String.valueOf(context.matchRequest().getId())
        );

        return ResponseUtil.success(
                new CaseMessageResponse(saved),
                "Message sent",
                HttpStatus.CREATED
        );
    }
}
