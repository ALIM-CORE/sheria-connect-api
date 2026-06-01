package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.exceptions.MatchingRequestNotFoundException;
import co.tz.sheriaconnectapi.exceptions.ProviderProfileNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedProviderProfileAccessException;
import co.tz.sheriaconnectapi.model.Entities.CaseMatchRequest;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.CaseMatchRequestRepository;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProviderCaseRequestAccessService {

    private final ProviderProfileAccessService providerProfileAccessService;
    private final ProviderProfileRepository providerProfileRepository;
    private final CaseMatchRequestRepository caseMatchRequestRepository;

    public ProviderCaseRequestAccessService(
            ProviderProfileAccessService providerProfileAccessService,
            ProviderProfileRepository providerProfileRepository,
            CaseMatchRequestRepository caseMatchRequestRepository
    ) {
        this.providerProfileAccessService = providerProfileAccessService;
        this.providerProfileRepository = providerProfileRepository;
        this.caseMatchRequestRepository = caseMatchRequestRepository;
    }

    public ProviderProfile requireMyProviderProfile(Authentication authentication) {
        User user = providerProfileAccessService.requireAuthenticatedUser(authentication);
        return providerProfileRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .orElseThrow(ProviderProfileNotFoundException::new);
    }

    public CaseMatchRequest requireMyRequest(Long matchingRequestId, Authentication authentication) {
        ProviderProfile profile = requireMyProviderProfile(authentication);
        CaseMatchRequest request = caseMatchRequestRepository.findDetailById(matchingRequestId)
                .orElseThrow(MatchingRequestNotFoundException::new);

        if (!request.getProviderProfile().getId().equals(profile.getId())) {
            throw new UnauthorizedProviderProfileAccessException();
        }

        return request;
    }
}
