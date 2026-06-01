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
        User savedUser = accountRegistrationService.register(input.getUser(), CITIZEN_ROLE);

        return ResponseUtil.success(
                new UserDTO(savedUser),
                "Registration successful. Please verify your email.",
                HttpStatus.CREATED
        );
    }
}
