package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.ProviderRegisterRequest;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.ProviderAvailabilityStatus;
import co.tz.sheriaconnectapi.model.Enums.ProviderVerificationStatus;
import co.tz.sheriaconnectapi.repositories.ProviderProfileRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RegisterProviderService implements Command<ProviderRegisterRequest, UserDTO> {

    private static final String PROVIDER_ROLE = "PROVIDER";

    private final AccountRegistrationService accountRegistrationService;
    private final ProviderProfileRepository providerProfileRepository;

    public RegisterProviderService(
            AccountRegistrationService accountRegistrationService,
            ProviderProfileRepository providerProfileRepository
    ) {
        this.accountRegistrationService = accountRegistrationService;
        this.providerProfileRepository = providerProfileRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<UserDTO>> execute(ProviderRegisterRequest request) {
        if (request == null || request.getProviderType() == null) {
            throw new UserNotValidException("Provider type is required");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());

        RegistrationResult result = accountRegistrationService.register(user, PROVIDER_ROLE);
        User savedUser = result.user();

        ProviderProfile profile = new ProviderProfile();
        profile.setUser(savedUser);
        profile.setProviderType(request.getProviderType());
        profile.setDisplayName(savedUser.getName());
        profile.setEmail(savedUser.getEmail());
        profile.setPhone(trimToNull(request.getPhone()));
        profile.setVerificationStatus(ProviderVerificationStatus.PENDING);
        profile.setAvailabilityStatus(ProviderAvailabilityStatus.UNAVAILABLE);
        profile.setActive(true);
        providerProfileRepository.save(profile);

        return ResponseUtil.success(
                new UserDTO(savedUser),
                registrationMessage(result.verificationEmailSent()),
                HttpStatus.CREATED
        );
    }

    private String registrationMessage(boolean verificationEmailSent) {
        if (verificationEmailSent) {
            return "Provider registration successful. Please verify your email and complete your profile.";
        }

        return "Provider registration successful, but the verification email could not be sent right now. Please request a new verification email later.";
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
