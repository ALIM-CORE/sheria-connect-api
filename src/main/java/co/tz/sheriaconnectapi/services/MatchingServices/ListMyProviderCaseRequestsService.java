package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.MatchingRequestResponse;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListMyProviderCaseRequestsService
        implements Query<Authentication, List<MatchingRequestResponse>> {

    private final ProviderCaseRequestAccessService accessService;
    private final CaseMatchRequestRepository caseMatchRequestRepository;

    public ListMyProviderCaseRequestsService(
            ProviderCaseRequestAccessService accessService,
            CaseMatchRequestRepository caseMatchRequestRepository
    ) {
        this.accessService = accessService;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<MatchingRequestResponse>>> execute(
            Authentication authentication
    ) {
        ProviderProfile profile = accessService.requireMyProviderProfile(authentication);
        List<MatchingRequestResponse> requests = caseMatchRequestRepository
                .findByProviderProfileOrderByCreatedAtDesc(profile)
                .stream()
                .map(MatchingRequestResponse::new)
                .toList();

        return ResponseUtil.success(requests, "Case requests retrieved", HttpStatus.OK);
    }
}
