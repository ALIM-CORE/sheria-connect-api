package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.model.DTOs.RegisterInput;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class RegisterService implements Command<RegisterInput, UserDTO> {

    private static final String CITIZEN_ROLE = "CITIZEN";

    private final AccountRegistrationService accountRegistrationService;

    public RegisterService(AccountRegistrationService accountRegistrationService) {
        this.accountRegistrationService = accountRegistrationService;
    }

    @Override
    public ResponseEntity<StandardResponse<UserDTO>> execute(RegisterInput input) {
        RegistrationResult result = accountRegistrationService.register(input.getUser(), CITIZEN_ROLE);
        User savedUser = result.user();

        return ResponseUtil.success(
                new UserDTO(savedUser),
                registrationMessage(result.verificationEmailSent()),
                HttpStatus.CREATED
        );
    }

    private String registrationMessage(boolean verificationEmailSent) {
        if (verificationEmailSent) {
            return "Registration successful. Please verify your email.";
        }

        return "Registration successful, but the verification email could not be sent right now. Please request a new verification email later.";
    }
}
