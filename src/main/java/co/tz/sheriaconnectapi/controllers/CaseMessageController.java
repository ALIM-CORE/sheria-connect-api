package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.CaseMessageInput;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageRequest;
import co.tz.sheriaconnectapi.model.DTOs.CaseMessageResponse;
import co.tz.sheriaconnectapi.services.MessagingServices.ListCitizenCaseMessagesService;
import co.tz.sheriaconnectapi.services.MessagingServices.ListProviderCaseMessagesService;
import co.tz.sheriaconnectapi.services.MessagingServices.SendCitizenCaseMessageService;
import co.tz.sheriaconnectapi.services.MessagingServices.SendProviderCaseMessageService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CaseMessageController {

    private final ListCitizenCaseMessagesService listCitizenCaseMessagesService;
    private final SendCitizenCaseMessageService sendCitizenCaseMessageService;
    private final ListProviderCaseMessagesService listProviderCaseMessagesService;
    private final SendProviderCaseMessageService sendProviderCaseMessageService;

    public CaseMessageController(
            ListCitizenCaseMessagesService listCitizenCaseMessagesService,
            SendCitizenCaseMessageService sendCitizenCaseMessageService,
            ListProviderCaseMessagesService listProviderCaseMessagesService,
            SendProviderCaseMessageService sendProviderCaseMessageService
    ) {
        this.listCitizenCaseMessagesService = listCitizenCaseMessagesService;
        this.sendCitizenCaseMessageService = sendCitizenCaseMessageService;
        this.listProviderCaseMessagesService = listProviderCaseMessagesService;
        this.sendProviderCaseMessageService = sendProviderCaseMessageService;
    }

    @GetMapping("/incident-reports/{caseNumber}/messages")
    public ResponseEntity<StandardResponse<List<CaseMessageResponse>>> listCitizenMessages(
            @PathVariable String caseNumber,
            @RequestParam(required = false) Long afterId,
            Authentication authentication
    ) {
        return listCitizenCaseMessagesService.execute(
                new CaseMessageInput(caseNumber, null, afterId, null, authentication)
        );
    }

    @PostMapping("/incident-reports/{caseNumber}/messages")
    public ResponseEntity<StandardResponse<CaseMessageResponse>> sendCitizenMessage(
            @PathVariable String caseNumber,
            @RequestBody CaseMessageRequest request,
            Authentication authentication
    ) {
        return sendCitizenCaseMessageService.execute(
                new CaseMessageInput(caseNumber, null, null, request, authentication)
        );
    }

    @GetMapping("/provider/case-requests/{matchingRequestId}/messages")
    public ResponseEntity<StandardResponse<List<CaseMessageResponse>>> listProviderMessages(
            @PathVariable Long matchingRequestId,
            @RequestParam(required = false) Long afterId,
            Authentication authentication
    ) {
        return listProviderCaseMessagesService.execute(
                new CaseMessageInput(null, matchingRequestId, afterId, null, authentication)
        );
    }

    @PostMapping("/provider/case-requests/{matchingRequestId}/messages")
    public ResponseEntity<StandardResponse<CaseMessageResponse>> sendProviderMessage(
            @PathVariable Long matchingRequestId,
            @RequestBody CaseMessageRequest request,
            Authentication authentication
    ) {
        return sendProviderCaseMessageService.execute(
                new CaseMessageInput(null, matchingRequestId, null, request, authentication)
        );
    }
}
