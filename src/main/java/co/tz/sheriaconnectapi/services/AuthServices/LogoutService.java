package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.InvalidClientTypeException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.LogoutInput;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutService implements Command<LogoutInput, Void> {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    @Override
    public ResponseEntity<StandardResponse<Void>> execute(
            LogoutInput input
    ) {

        if (input.getAuth() == null || !input.getAuth().isAuthenticated()) {
            return ResponseUtil.error(
                    "Unauthorized",
                    HttpStatus.UNAUTHORIZED
            );
        }

        User user = userRepository
                .findByEmail(input.getAuth().getName())
                .orElseThrow(UserNotFoundException::new);

        refreshTokenRepository.deleteAllByUserId(user.getId());

        String clientHeader =
                input.getRequest().getHeader("X-Client-Type");

        ClientType clientType = ClientType.WEB;

        if (clientHeader != null) {
            try {
                clientType = ClientType.valueOf(
                        clientHeader.toUpperCase()
                );
            } catch (IllegalArgumentException e) {
                throw new InvalidClientTypeException();
            }
        }

        if (clientType == ClientType.WEB) {
            Cookie cookie = new Cookie("refresh_token", "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/auth/refresh");
            cookie.setMaxAge(0);
            input.getResponse().addCookie(cookie);
        }

        SecurityContextHolder.clearContext();

        return ResponseUtil.success(
                null,
                "Logout successful",
                HttpStatus.OK
        );
    }
}