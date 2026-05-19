package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.ProviderProfileNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class GetMyProviderProfileService implements Query<Authentication, ProviderProfileResponse> {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderProfileAccessService providerProfileAccessService;

    public GetMyProviderProfileService(
            ProviderProfileRepository providerProfileRepository,
            ProviderProfileAccessService providerProfileAccessService
    ) {
        this.providerProfileRepository = providerProfileRepository;
        this.providerProfileAccessService = providerProfileAccessService;
    }

    @Override
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> execute(Authentication authentication) {
        User user = providerProfileAccessService.requireAuthenticatedUser(authentication);
        ProviderProfile profile = providerProfileRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .orElseThrow(ProviderProfileNotFoundException::new);

        return ResponseUtil.success(
                new ProviderProfileResponse(profile),
                "Provider profile retrieved successfully",
                HttpStatus.OK
        );
    }
}
