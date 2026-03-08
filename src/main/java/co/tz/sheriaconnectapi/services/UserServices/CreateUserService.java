package co.tz.sheriaconnectapi.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;

@Service
public class CreateUserService implements Command<User, UserDTO> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CreateUserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<StandardResponse<UserDTO>> execute(User user) {
        return execute(user, null);
    }

    public ResponseEntity<StandardResponse<UserDTO>> execute(
            User user,
            HttpServletRequest request
    ) {

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserNotValidException(
                    ErrorMessages.EMAIL_ALREADY_EXISTS.getMessage()
            );
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        user = userRepository.save(user);

        UserDTO dto = new UserDTO(user);

        return ResponseUtil.success(
                dto,
                "User created successfully",
                HttpStatus.CREATED
        );
    }
}
