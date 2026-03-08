package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.*;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.services.AuthServices.*;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final LoginService loginService;
    private final RefreshService refreshService;
    private final LogoutService logoutService;
    private final VerifyEmailService  verifyEmailService;
    private final RequestPasswordResetService requestPasswordResetService;
    private final ResetPasswordService resetPasswordService;
    private final RegisterService registerService;
    private final ResendVerificationEmailService resendVerificationEmailService;

    public AuthController(
            LoginService loginService,
            RefreshService refreshService,
            LogoutService logoutService,
            VerifyEmailService verifyEmailService,
            RequestPasswordResetService requestPasswordResetService,
            ResetPasswordService resetPasswordService,
            RegisterService registerService,
            ResendVerificationEmailService resendVerificationEmailService
    ) {
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.verifyEmailService = verifyEmailService;
        this.requestPasswordResetService = requestPasswordResetService;
        this.resetPasswordService = resetPasswordService;
        this.registerService = registerService;
        this.resendVerificationEmailService = resendVerificationEmailService;
    }

    @PostMapping("/login")
    public ResponseEntity<StandardResponse<LoginResponse>> login(
            @RequestBody UserLoginDTO dto,
            HttpServletRequest request
    ) {
        return loginService.execute(new LoginInput(dto, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<StandardResponse<Map<String, Object>>> refresh(
            @RequestBody(required = false) RefreshTokenRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return refreshService.execute(
                new RefreshInput(request, response, body)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<StandardResponse<Void>> logout(
            Authentication auth,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return logoutService.execute(
                new LogoutInput(auth, request, response)
        );
    }

    @GetMapping("/verify-email")
    public ResponseEntity<StandardResponse<Void>> verifyEmail(@RequestParam String token) {
        return verifyEmailService.execute(
                new VerifyEmailInput(token)
        );
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<StandardResponse<Void>> requestReset(
            @RequestBody PasswordResetRequestDTO dto
    ) {
        return requestPasswordResetService.execute(dto);
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<StandardResponse<Void>> resetPassword(
            @RequestBody ResetPasswordDTO dto
    ) {
        return resetPasswordService.execute(dto);
    }

    @PostMapping("/register")
    public ResponseEntity<StandardResponse<UserDTO>> register(
            @RequestBody User user,
            HttpServletRequest request
    ) {
        return registerService.execute(
                new RegisterInput(user, request)
        );
    }

    @PostMapping("/verify-email/resend")
    public ResponseEntity<StandardResponse<Void>> resendVerificationEmail(
            @RequestBody ResendVerificationEmailDTO dto
    ) {
        return resendVerificationEmailService.execute(dto);
    }


}

