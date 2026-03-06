package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.EmailNotVerifiedException;
import co.tz.sheriaconnectapi.exceptions.InvalidClientTypeException;
import co.tz.sheriaconnectapi.exceptions.InvalidLoginCredentialsException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.Commands.MobileLoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.LoginInput;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class LoginService implements Command<LoginInput, LoginResponse> {

    private final AuthenticationManager manager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public LoginService(
            AuthenticationManager manager,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.manager = manager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<LoginResponse>> execute(LoginInput loginInput) {

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        loginInput.getUserLoginDTO().getEmail(),
                        loginInput.getUserLoginDTO().getPassword()
                );

        Authentication authentication;

        try {
            authentication = manager.authenticate(authToken);
        } catch (EmailNotVerifiedException ex) {
            System.out.println("LOGIN AUTH EXCEPTION: " + ex.getClass().getName());
            System.out.println("LOGIN AUTH MESSAGE: " + ex.getMessage());
            System.out.println("LOGIN AUTH CAUSE: null");
            throw ex;
        } catch (BadCredentialsException | InternalAuthenticationServiceException ex) {
            System.out.println("LOGIN AUTH EXCEPTION: " + ex.getClass().getName());
            System.out.println("LOGIN AUTH MESSAGE: " + ex.getMessage());
            System.out.println(
                    "LOGIN AUTH CAUSE: " +
                            (ex.getCause() != null ? ex.getCause().getClass().getName() : "null")
            );

            Throwable cause = ex.getCause();

            if (cause instanceof EmailNotVerifiedException emailNotVerifiedException) {
                throw emailNotVerifiedException;
            }

            throw new InvalidLoginCredentialsException();
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String clientHeader = loginInput.getRequest().getHeader("X-Client-Type");
        ClientType clientType = ClientType.WEB;

        if (clientHeader != null) {
            try {
                clientType = ClientType.valueOf(clientHeader.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidClientTypeException();
            }
        }

        String accessToken = JwtUtil.generateAccessToken(userDetails, clientType);
        String refreshToken = JwtUtil.generateRefreshToken(userDetails, clientType);

        User userEntity = userRepository.findByEmail(
                loginInput.getUserLoginDTO().getEmail()
        ).orElseThrow(UserNotFoundException::new);

        RefreshService.storeNewRefreshToken(
                userEntity,
                clientType,
                refreshToken,
                refreshTokenRepository
        );

        UserDTO userDTO = new UserDTO(userEntity);

        if (clientType == ClientType.WEB) {
            LoginResponse loginBody = new LoginResponse(
                    accessToken,
                    userDTO
            );

            return ResponseEntity.ok()
                    .header(
                            "Set-Cookie",
                            "refresh_token=" + refreshToken +
                                    "; HttpOnly; Path=/auth/refresh" +
                                    "; Max-Age=" + (7 * 24 * 60 * 60) +
                                    "; SameSite=None; Secure"
                    )
                    .body(
                            new StandardResponse<>(
                                    true,
                                    "Login successful, you're using web!",
                                    loginBody,
                                    null
                            )
                    );
        }

        MobileLoginResponse mobileBody = new MobileLoginResponse(
                accessToken,
                userDTO,
                refreshToken
        );

        return ResponseUtil.success(
                mobileBody,
                "Login successful, you're using mobile!",
                HttpStatus.OK
        );
    }
}