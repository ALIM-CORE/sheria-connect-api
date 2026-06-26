package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.ProviderProfileNotFoundException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.ProviderProfileResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateProviderVerificationInput;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Enums.NotificationType;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.services.NotificationServices.NotificationDispatchService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class UpdateProviderVerificationService
        implements Command<UpdateProviderVerificationInput, ProviderProfileResponse> {

    private final ProviderProfileRepository providerProfileRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final UserRepository userRepository;

    public UpdateProviderVerificationService(
            ProviderProfileRepository providerProfileRepository,
            NotificationDispatchService notificationDispatchService,
            UserRepository userRepository
    ) {
        this.providerProfileRepository = providerProfileRepository;
        this.notificationDispatchService = notificationDispatchService;
        this.userRepository = userRepository;
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
        var actor = input.authentication() == null
                ? null
                : userRepository.findByEmail(input.authentication().getName()).orElse(null);
        if (actor != null
                && providerProfile.getUser() != null
                && actor.getId().equals(providerProfile.getUser().getId())) {
            throw new AccessManagementException(
                    "You cannot review your own provider verification",
                    HttpStatus.FORBIDDEN
            );
        }

        providerProfile.setVerificationStatus(input.request().getVerificationStatus());
        providerProfile.setVerificationRejectionReason(
                input.request().getVerificationStatus() == ProviderVerificationStatus.REJECTED
                        ? trimToNull(input.request().getRejectionReason())
                        : null
        );
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
        notificationDispatchService.notify(
                saved.getUser(),
                AccessContext.PROVIDER,
                NotificationType.PROVIDER_VERIFICATION_DECISION,
                "Provider verification updated",
                "Your provider profile is now "
                        + saved.getVerificationStatus().name().toLowerCase().replace('_', ' ') + ".",
                "PROVIDER_PROFILE",
                String.valueOf(saved.getId())
        );

        return ResponseUtil.success(
                new ProviderProfileResponse(saved),
                "Provider profile updated successfully",
                HttpStatus.OK
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
