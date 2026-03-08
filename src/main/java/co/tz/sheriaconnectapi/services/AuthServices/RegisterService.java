package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.DTOs.RegisterInput;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.EmailVerificationToken;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.EmailVerificationTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.services.EmailService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;

import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class RegisterService implements Command<RegisterInput, UserDTO> {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public RegisterService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<StandardResponse<UserDTO>> execute(RegisterInput input) {

        User user = input.getUser();

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserNotValidException(
                    ErrorMessages.EMAIL_ALREADY_EXISTS.getMessage()
            );
        }

        // 1️⃣ Secure password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);

        // 2️⃣ Save user
        User savedUser = userRepository.save(user);

        // 3️⃣ Remove old tokens
        tokenRepository.deleteByUserId(savedUser.getId());

        // 4️⃣ Create verification token
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(savedUser);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(
                Instant.now().plusSeconds(24 * 60 * 60)
        );

        tokenRepository.save(token);

        // 5️⃣ Send verification email
        String verificationLink =
                frontendBaseUrl +
                        "/auth/verify-email?token=" + token.getToken();

        emailService.sendEmailVerification(
                savedUser.getEmail(),
                savedUser.getName(),
                verificationLink
        );

        UserDTO dto = new UserDTO(savedUser);

        return ResponseUtil.success(
                dto,
                "Registration successful. Please verify your email.",
                HttpStatus.CREATED
        );
    }
}
