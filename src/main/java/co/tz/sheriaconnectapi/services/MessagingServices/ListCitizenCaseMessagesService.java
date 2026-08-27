package co.tz.sheriaconnectapi.services.MessagingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageResponse;
import co.tz.sheriaconnectapi.repositories.CaseMessageRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListCitizenCaseMessagesService
        implements Query<CaseMessageInput, List<CaseMessageResponse>> {

    private final CaseMessageAccessService accessService;
    private final CaseMessageRepository caseMessageRepository;

    public ListCitizenCaseMessagesService(
            CaseMessageAccessService accessService,
            CaseMessageRepository caseMessageRepository
    ) {
        this.accessService = accessService;
        this.caseMessageRepository = caseMessageRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<CaseMessageResponse>>> execute(
            CaseMessageInput input
    ) {
        var context = accessService.requireCitizenContext(input.caseNumber(), input.authentication());
        List<CaseMessageResponse> messages = citizenMessages(input, context)
                .stream()
                .map(CaseMessageResponse::new)
                .toList();

        return ResponseUtil.success(messages, "Case messages retrieved", HttpStatus.OK);
    }

    private List<co.tz.sheriaconnectapi.model.Entities.CaseMessage> citizenMessages(
            CaseMessageInput input,
            CaseMessageAccessService.CitizenMessageContext context
    ) {
        if (input.afterId() != null) {
            return caseMessageRepository.findByIncidentReportAndIdGreaterThanOrderByCreatedAtAsc(
                    context.report(),
                    input.afterId()
            );
        }
        return caseMessageRepository.findByIncidentReportOrderByCreatedAtAsc(context.report());
    }
}
