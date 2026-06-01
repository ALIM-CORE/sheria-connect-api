package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidMatchingRequestStatusException;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateMatchingRequestStatusInput;
import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class UpdateMyProviderCaseRequestStatusService
        implements Command<UpdateMatchingRequestStatusInput, MatchingRequestResponse> {

    private static final Set<MatchingRequestStatus> PROVIDER_ALLOWED =
            Set.of(MatchingRequestStatus.ACCEPTED, MatchingRequestStatus.DECLINED);

    private final ProviderCaseRequestAccessService accessService;
    private final UpdateMatchingRequestStatusService updateMatchingRequestStatusService;

    public UpdateMyProviderCaseRequestStatusService(
            ProviderCaseRequestAccessService accessService,
            UpdateMatchingRequestStatusService updateMatchingRequestStatusService
    ) {
        this.accessService = accessService;
        this.updateMatchingRequestStatusService = updateMatchingRequestStatusService;
    }

    @Override
    public ResponseEntity<StandardResponse<MatchingRequestResponse>> execute(
            UpdateMatchingRequestStatusInput input
    ) {
        accessService.requireMyRequest(input.matchingRequestId(), input.authentication());

        if (input.request() == null
                || input.request().getStatus() == null
                || !PROVIDER_ALLOWED.contains(input.request().getStatus())) {
            throw new InvalidMatchingRequestStatusException();
        }

        return updateMatchingRequestStatusService.execute(input);
    }
}
