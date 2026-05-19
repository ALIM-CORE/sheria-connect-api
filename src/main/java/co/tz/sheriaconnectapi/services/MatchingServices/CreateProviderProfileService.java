package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.CreateProviderProfileRequest;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.PricingTier;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CreateProviderProfileService
        implements Command<CreateProviderProfileRequest, ProviderProfileResponse> {

    private final ProviderProfileRepository providerProfileRepository;
    private final UserRepository userRepository;

    public CreateProviderProfileService(
            ProviderProfileRepository providerProfileRepository,
            UserRepository userRepository
    ) {
        this.providerProfileRepository = providerProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> execute(
            CreateProviderProfileRequest request
    ) {
        if (request == null ||
                request.getProviderType() == null ||
                request.getDisplayName() == null ||
                request.getDisplayName().isBlank()) {
            throw new UserNotValidException("Provider type and display name are required");
        }

        ProviderProfile providerProfile = new ProviderProfile();
        if (request.getUserId() != null) {
            providerProfile.setUser(
                    userRepository.findById(request.getUserId())
                            .orElseThrow(UserNotFoundException::new)
            );
        }

        providerProfile.setProviderType(request.getProviderType());
        providerProfile.setDisplayName(request.getDisplayName().trim());
        providerProfile.setOrganizationName(trimToNull(request.getOrganizationName()));
        providerProfile.setEmail(trimToNull(request.getEmail()));
        providerProfile.setPhone(trimToNull(request.getPhone()));
        providerProfile.setVerificationStatus(
                request.getVerificationStatus() == null
                        ? ProviderVerificationStatus.PENDING
                        : request.getVerificationStatus()
        );
        providerProfile.setAvailabilityStatus(
                request.getAvailabilityStatus() == null
                        ? ProviderAvailabilityStatus.AVAILABLE
                        : request.getAvailabilityStatus()
        );
        providerProfile.setPricingTier(
                request.getPricingTier() == null
                        ? PricingTier.FREE
                        : request.getPricingTier()
        );
        providerProfile.setActive(request.getActive() == null || request.getActive());
        providerProfile.setCurrentWorkload(nonNegative(request.getCurrentWorkload(), 0));
        providerProfile.setMaxActiveCases(nonNegative(request.getMaxActiveCases(), 10));
        providerProfile.setNotes(trimToNull(request.getNotes()));

        if (request.getSpecialties() != null) {
            providerProfile.setSpecialties(request.getSpecialties());
        }

        if (request.getRegions() != null) {
            providerProfile.setRegions(
                    request.getRegions().stream()
                            .filter(region -> region != null && !region.isBlank())
                            .map(region -> region.trim().toLowerCase(Locale.ROOT))
                            .collect(Collectors.toSet())
            );
        }

        ProviderProfile saved = providerProfileRepository.save(providerProfile);
        return ResponseUtil.success(
                new ProviderProfileResponse(saved),
                "Provider profile created successfully",
                HttpStatus.CREATED
        );
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
