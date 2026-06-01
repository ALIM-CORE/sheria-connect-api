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
public class ListProviderCaseMessagesService
        implements Query<CaseMessageInput, List<CaseMessageResponse>> {

    private final CaseMessageAccessService accessService;
    private final CaseMessageRepository caseMessageRepository;

    public ListProviderCaseMessagesService(
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
        var context = accessService.requireProviderContext(
                input.matchingRequestId(),
                input.authentication()
        );
        List<CaseMessageResponse> messages = caseMessageRepository
                .findByCaseMatchRequestOrderByCreatedAtAsc(context.matchRequest())
                .stream()
                .map(CaseMessageResponse::new)
                .toList();

        return ResponseUtil.success(messages, "Case messages retrieved", HttpStatus.OK);
    }
}
