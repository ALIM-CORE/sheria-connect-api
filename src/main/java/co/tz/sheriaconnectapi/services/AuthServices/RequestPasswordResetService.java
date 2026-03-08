package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.model.DTOs.PasswordResetRequestDTO;
import co.tz.sheriaconnectapi.model.Entities.PasswordResetToken;
import co.tz.sheriaconnectapi.repositories.PasswordResetTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.services.EmailService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RequestPasswordResetService
        implements Command<PasswordResetRequestDTO, Void> {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.base-domain}")
    private String frontendBaseUrl;

    public RequestPasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    @Override
    public ResponseEntity<StandardResponse<Void>> execute(
            PasswordResetRequestDTO input
    ) {

        userRepository.findByEmail(input.getEmail())
                .ifPresent(user -> {

                    tokenRepository.deleteAllByUserId(user.getId());

                    PasswordResetToken token = new PasswordResetToken();
                    token.setUser(user);
                    token.setToken(UUID.randomUUID().toString());
                    token.setExpiryDate(
                            Instant.now().plusSeconds(15 * 60)
                    );

                    tokenRepository.save(token);

                    String resetLink =
                            frontendBaseUrl
                                    + "/auth/reset-password?token="
                                    + token.getToken();

                    emailService.sendPasswordResetEmail(
                            user.getEmail(),
                            user.getName(),
                            resetLink
                    );
                });

        // 🔐 Always same response (anti-enumeration protection)
        return ResponseUtil.success(
                null,
                "If the email exists, a reset link was sent",
                HttpStatus.OK
        );
    }
}
