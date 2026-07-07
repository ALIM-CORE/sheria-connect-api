package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.EmailDeliveryException;
import co.tz.sheriaconnectapi.exceptions.UserNotValidException;
import co.tz.sheriaconnectapi.model.Entities.EmailVerificationToken;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.repositories.EmailVerificationTokenRepository;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AccountRegistrationService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountRegistrationService.class);

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final UserRoleAssignmentRepository assignmentRepository;

    @Value("${app.frontend.base-url}")
    private String backendBaseUrl;

    @Value("${app.frontend.base-domain}")
    private String frontendBaseDomain;

    public AccountRegistrationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            UserRoleAssignmentRepository assignmentRepository
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.assignmentRepository = assignmentRepository;
    }

    public RegistrationResult register(User user, String roleName) {
        normalize(user);

        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserNotValidException(ErrorMessages.EMAIL_ALREADY_EXISTS.getMessage());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmailVerified(false);
        user.setAccountType("PROVIDER".equals(roleName)
                ? UserAccountType.PROVIDER
                : UserAccountType.CITIZEN);
        user.setActive(true);
        user.setLocked(false);
        Role identityRole = roleRepository.findByName(roleName).orElse(null);
        if (identityRole != null) {
            user.getRoles().add(identityRole);
        }

        User savedUser = userRepository.save(user);
        if (identityRole != null) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(savedUser);
            assignment.setRole(identityRole);
            assignment.setContext("PROVIDER".equals(roleName)
                    ? AccessContext.PROVIDER
                    : AccessContext.CITIZEN);
            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
            assignment.setActivatedAt(Instant.now());
            assignment.setReason("Created during account registration");
            assignmentRepository.save(assignment);
        }
        tokenRepository.deleteByUserId(savedUser.getId());
        EmailVerificationToken token = createVerificationToken(savedUser);
        tokenRepository.save(token);
        boolean verificationEmailSent = sendVerificationEmail(savedUser, token);

        return new RegistrationResult(savedUser, verificationEmailSent);
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

    private boolean sendVerificationEmail(User savedUser, EmailVerificationToken token) {
        String verificationLink = UriComponentsBuilder
                .fromUriString(frontendBaseDomain)
                .path("/auth/verify-email")
                .queryParam("token", token.getToken())
                .queryParam("email", savedUser.getEmail())
                .queryParam("api_base_url", backendBaseUrl)
                .build()
                .encode()
                .toUriString();

        try {
            emailService.sendEmailVerification(
                    savedUser.getEmail(),
                    savedUser.getName(),
                    verificationLink
            );
            return true;
        } catch (EmailDeliveryException ex) {
            log.warn(
                    "Verification email could not be sent during registration for user {}",
                    savedUser.getId(),
                    ex
            );
            return false;
        }
    }
}
