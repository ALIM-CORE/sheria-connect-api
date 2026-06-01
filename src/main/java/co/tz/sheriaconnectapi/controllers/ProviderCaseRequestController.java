package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusInput;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusRequest;
import co.tz.sheriaconnectapi.services.MatchingServices.GetMyProviderCaseRequestService;
import co.tz.sheriaconnectapi.services.MatchingServices.ListMyProviderCaseRequestsService;
import co.tz.sheriaconnectapi.services.MatchingServices.UpdateMyProviderCaseRequestStatusService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/provider/case-requests")
public class ProviderCaseRequestController {

    private final ListMyProviderCaseRequestsService listMyProviderCaseRequestsService;
    private final GetMyProviderCaseRequestService getMyProviderCaseRequestService;
    private final UpdateMyProviderCaseRequestStatusService updateMyProviderCaseRequestStatusService;

    public ProviderCaseRequestController(
            ListMyProviderCaseRequestsService listMyProviderCaseRequestsService,
            GetMyProviderCaseRequestService getMyProviderCaseRequestService,
            UpdateMyProviderCaseRequestStatusService updateMyProviderCaseRequestStatusService
    ) {
        this.listMyProviderCaseRequestsService = listMyProviderCaseRequestsService;
        this.getMyProviderCaseRequestService = getMyProviderCaseRequestService;
        this.updateMyProviderCaseRequestStatusService = updateMyProviderCaseRequestStatusService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<List<MatchingRequestResponse>>> list(
            Authentication authentication
    ) {
        return listMyProviderCaseRequestsService.execute(authentication);
    }

    @GetMapping("/{matchingRequestId}")
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> get(
            @PathVariable Long matchingRequestId,
            Authentication authentication
    ) {
        return getMyProviderCaseRequestService.execute(
                new UpdateMatchingRequestStatusInput(matchingRequestId, null, authentication)
        );
    }

    @PatchMapping("/{matchingRequestId}/status")
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> updateStatus(
            @PathVariable Long matchingRequestId,
            @RequestBody UpdateMatchingRequestStatusRequest request,
            Authentication authentication
    ) {
        return updateMyProviderCaseRequestStatusService.execute(
                new UpdateMatchingRequestStatusInput(matchingRequestId, request, authentication)
        );
    }
}
