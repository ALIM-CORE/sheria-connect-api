package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.EmailAlreadyVerifiedException;
import co.tz.sheriaconnectapi.model.DTOs.ResendVerificationEmailDTO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResendVerificationEmailService
        implements Command<ResendVerificationEmailDTO, Void> {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public ResendVerificationEmailService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    @Override
    public ResponseEntity<StandardResponse<Void>> execute(
            ResendVerificationEmailDTO input
    ) {
        User user = userRepository.findByEmail(input.getEmail()).orElse(null);

        // Avoid leaking whether the email exists
        if (user == null) {
            return ResponseUtil.success(
                    null,
                    "If the account exists and is not yet verified, a new verification email has been sent.",
                    HttpStatus.OK
            );
        }

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new EmailAlreadyVerifiedException();
        }

        EmailVerificationToken token = tokenRepository.findByUserId(user.getId())
                .orElseGet(EmailVerificationToken::new);

        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusSeconds(24 * 60 * 60));

        tokenRepository.save(token);

        String verificationLink =
                frontendBaseUrl + "/auth/verify-email?token=" + token.getToken();

        emailService.sendEmailVerification(
                user.getEmail(),
                user.getName(),
                verificationLink
        );

        return ResponseUtil.success(
                null,
                "Verification email sent successfully.",
                HttpStatus.OK
        );
    }
}