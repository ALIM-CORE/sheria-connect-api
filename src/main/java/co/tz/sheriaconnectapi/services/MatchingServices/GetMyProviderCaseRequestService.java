package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusInput;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetMyProviderCaseRequestService
        implements Query<UpdateMatchingRequestStatusInput, MatchingRequestResponse> {

    private final ProviderCaseRequestAccessService accessService;

    public GetMyProviderCaseRequestService(ProviderCaseRequestAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> execute(
            UpdateMatchingRequestStatusInput input
    ) {
        return ResponseUtil.success(
                new MatchingRequestResponse(
                        accessService.requireMyRequest(input.matchingRequestId(), input.authentication())
                ),
                "Case request retrieved",
                HttpStatus.OK
        );
    }
}
