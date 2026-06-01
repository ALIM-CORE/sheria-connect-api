package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.Entities.EmailVerificationToken;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.EmailVerificationTokenRepository;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.services.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountRegistrationService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String backendBaseUrl;

    @Value("${app.frontend.base-domain}")
    private String frontendBaseDomain;

    public AccountRegistrationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User register(User user, String roleName) {
        normalize(user);

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserNotValidException(ErrorMessages.EMAIL_ALREADY_EXISTS.getMessage());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);
        roleRepository.findByName(roleName)
                .ifPresent(role -> user.getRoles().add(role));

        User savedUser = userRepository.save(user);
        tokenRepository.deleteByUserId(savedUser.getId());
        EmailVerificationToken token = createVerificationToken(savedUser);
        tokenRepository.save(token);
        sendVerificationEmail(savedUser, token);

        return savedUser;
    }

    private void normalize(User user) {
        if (user == null
                || user.getName() == null
                || user.getName().isBlank()
                || user.getEmail() == null
                || user.getEmail().isBlank()
                || user.getPassword() == null
                || user.getPassword().isBlank()) {
            throw new UserNotValidException("Name, email, and password are required");
        }

        user.setName(user.getName().trim());
        user.setEmail(user.getEmail().trim().toLowerCase(Locale.ROOT));
    }

    private EmailVerificationToken createVerificationToken(User savedUser) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(savedUser);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusSeconds(24 * 60 * 60));
        return token;
    }

    private void sendVerificationEmail(User savedUser, EmailVerificationToken token) {
        String verificationLink = UriComponentsBuilder
                .fromUriString(frontendBaseDomain)
                .path("/auth/verify-email")
                .queryParam("token", token.getToken())
                .queryParam("email", savedUser.getEmail())
                .queryParam("api_base_url", backendBaseUrl)
                .build()
                .encode()
                .toUriString();

        emailService.sendEmailVerification(
                savedUser.getEmail(),
                savedUser.getName(),
                verificationLink
        );
    }
}
