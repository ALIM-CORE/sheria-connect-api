package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.EmailAlreadyVerifiedException;
import co.tz.sheriaconnectapi.exceptions.EmailVerificationTokenExpiredException;
import co.tz.sheriaconnectapi.exceptions.InvalidEmailVerificationTokenException;
import co.tz.sheriaconnectapi.model.DTOs.VerifyEmailInput;
import co.tz.sheriaconnectapi.model.Entities.EmailVerificationToken;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.EmailVerificationTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class VerifyEmailService
        implements Command<VerifyEmailInput, Void> {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    public VerifyEmailService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<Void>> execute(
            VerifyEmailInput input
    ) {

        EmailVerificationToken token = tokenRepository
                .findByToken(input.getToken())
                .orElseThrow(InvalidEmailVerificationTokenException::new);

        if (token.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new EmailVerificationTokenExpiredException();
        }

        User user = token.getUser();

        if (user.getEmailVerified()) {
            throw new EmailAlreadyVerifiedException();
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        tokenRepository.delete(token);

        return ResponseUtil.success(
                null,
                "Email verified successfully",
                HttpStatus.OK
        );
    }
}
