package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.ProviderProfileNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateProviderVerificationInput;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UpdateProviderVerificationService
        implements Command<UpdateProviderVerificationInput, ProviderProfileResponse> {

    private final ProviderProfileRepository providerProfileRepository;

    public UpdateProviderVerificationService(
            ProviderProfileRepository providerProfileRepository
    ) {
        this.providerProfileRepository = providerProfileRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<ProviderProfileResponse>> execute(
            UpdateProviderVerificationInput input
    ) {
        if (input.request() == null || input.request().getVerificationStatus() == null) {
            throw new UserNotValidException("Verification status is required");
        }

        ProviderProfile providerProfile = providerProfileRepository
                .findById(input.providerProfileId())
                .orElseThrow(ProviderProfileNotFoundException::new);

        providerProfile.setVerificationStatus(input.request().getVerificationStatus());
        if (input.request().getAvailabilityStatus() != null) {
            providerProfile.setAvailabilityStatus(input.request().getAvailabilityStatus());
        }
        if (input.request().getActive() != null) {
            providerProfile.setActive(input.request().getActive());
        }
        if (input.request().getNotes() != null) {
            providerProfile.setNotes(input.request().getNotes().trim());
        }

        ProviderProfile saved = providerProfileRepository.save(providerProfile);
        return ResponseUtil.success(
                new ProviderProfileResponse(saved),
                "Provider profile updated successfully",
                HttpStatus.OK
        );
    }
}
