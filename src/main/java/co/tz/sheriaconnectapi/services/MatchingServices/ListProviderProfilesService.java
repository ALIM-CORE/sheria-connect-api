package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileSearchInput;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListProviderProfilesService
        implements Query<ProviderProfileSearchInput, List<ProviderProfileResponse>> {

    private final ProviderProfileRepository providerProfileRepository;

    public ListProviderProfilesService(ProviderProfileRepository providerProfileRepository) {
        this.providerProfileRepository = providerProfileRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<ProviderProfileResponse>>> execute(
            ProviderProfileSearchInput input
    ) {
        List<ProviderProfileResponse> providerProfiles = providerProfileRepository
                .search(
                        input.providerType(),
                        input.verificationStatus(),
                        input.availabilityStatus(),
                        input.active()
                )
                .stream()
                .map(ProviderProfileResponse::new)
                .toList();

        return ResponseUtil.success(
                providerProfiles,
                "Provider profiles retrieved successfully",
                HttpStatus.OK
        );
    }
}
