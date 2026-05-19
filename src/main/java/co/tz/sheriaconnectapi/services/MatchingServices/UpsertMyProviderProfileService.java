package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.CreateProviderProfileRequest;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpsertMyProviderProfileInput;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class UpsertMyProviderProfileService
        implements Command<UpsertMyProviderProfileInput, ProviderProfileResponse> {

    private final ProviderProfileRepository providerProfileRepository;
    private final ProviderProfileAccessService providerProfileAccessService;

    public UpsertMyProviderProfileService(
            ProviderProfileRepository providerProfileRepository,
            ProviderProfileAccessService providerProfileAccessService
    ) {
        this.providerProfileRepository = providerProfileRepository;
        this.providerProfileAccessService = providerProfileAccessService;
    }

    @Override
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> execute(
            UpsertMyProviderProfileInput input
    ) {
        CreateProviderProfileRequest request = input.request();
        validate(request);

        User user = providerProfileAccessService.requireAuthenticatedUser(input.authentication());
        ProviderProfile profile = providerProfileRepository.findFirstByUserOrderByCreatedAtDesc(user)
                .orElseGet(ProviderProfile::new);

        boolean creating = profile.getId() == null;
        profile.setUser(user);
        profile.setProviderType(request.getProviderType());
        profile.setDisplayName(request.getDisplayName().trim());
        profile.setOrganizationName(trimToNull(request.getOrganizationName()));
        profile.setEmail(trimToNull(request.getEmail()));
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setVerificationStatus(ProviderVerificationStatus.PENDING);
        profile.setAvailabilityStatus(
                request.getAvailabilityStatus() == null
                        ? profile.getAvailabilityStatus()
                        : request.getAvailabilityStatus()
        );
        profile.setPricingTier(
                request.getPricingTier() == null ? profile.getPricingTier() : request.getPricingTier()
        );
        profile.setActive(true);
        profile.setCurrentWorkload(Math.max(profile.getCurrentWorkload(), 0));
        profile.setMaxActiveCases(nonNegative(
                request.getMaxActiveCases(),
                profile.getMaxActiveCases() > 0 ? profile.getMaxActiveCases() : 10
        ));
        profile.setNotes(null);

        profile.getSpecialties().clear();
        if (request.getSpecialties() != null) {
            profile.getSpecialties().addAll(request.getSpecialties());
        }

        profile.getRegions().clear();
        if (request.getRegions() != null) {
            profile.getRegions().addAll(
                    request.getRegions().stream()
                            .filter(region -> region != null && !region.isBlank())
                            .map(region -> region.trim().toLowerCase(Locale.ROOT))
                            .collect(Collectors.toSet())
            );
        }

        ProviderProfile saved = providerProfileRepository.save(profile);
        return ResponseUtil.success(
                new ProviderProfileResponse(saved),
                creating
                        ? "Provider profile submitted for verification"
                        : "Provider profile updated and queued for verification",
                creating ? HttpStatus.CREATED : HttpStatus.OK
        );
    }

    private void validate(CreateProviderProfileRequest request) {
        if (request == null
                || request.getProviderType() == null
                || request.getDisplayName() == null
                || request.getDisplayName().isBlank()) {
            throw new UserNotValidException("Provider type and display name are required");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int nonNegative(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(value, 0);
    }
}
