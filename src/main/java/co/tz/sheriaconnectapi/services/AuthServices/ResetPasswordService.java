package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidPasswordResetTokenException;
import co.tz.sheriaconnectapi.exceptions.PasswordResetTokenExpiredException;
import co.tz.sheriaconnectapi.model.DTOs.ResetPasswordDTO;
import co.tz.sheriaconnectapi.model.Entities.PasswordResetToken;
import co.tz.sheriaconnectapi.repositories.PasswordResetTokenRepository;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ResetPasswordService
        implements Command<ResetPasswordDTO, Void> {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override
    public ResponseEntity<StandardResponse<Void>> execute(
            ResetPasswordDTO input
    ) {

        PasswordResetToken token = tokenRepository
                .findByToken(input.getToken())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (token.getExpiryDate().isBefore(Instant.now())) {
            tokenRepository.delete(token);
            throw new PasswordResetTokenExpiredException();
        }

        var user = token.getUser();
        user.setPassword(passwordEncoder.encode(input.getNewPassword()));
        userRepository.save(user);

        // 🔐 Invalidate all sessions (force re-login everywhere)
        refreshTokenRepository.deleteAllByUserId(user.getId());

        tokenRepository.delete(token);

        return ResponseUtil.success(
                null,
                "Password reset successful",
                HttpStatus.OK
        );
    }
}
