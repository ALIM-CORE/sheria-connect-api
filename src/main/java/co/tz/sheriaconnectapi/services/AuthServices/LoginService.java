package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.abstractions.Command;
import co.tz.sheriaconnectapi.exceptions.EmailNotVerifiedException;
import co.tz.sheriaconnectapi.exceptions.InvalidClientTypeException;
import co.tz.sheriaconnectapi.exceptions.InvalidLoginCredentialsException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.exceptions.WebPortalAccessDeniedException;
import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.Commands.MobileLoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.LoginInput;
import co.tz.sheriaconnectapi.model.DTOs.UserDTO;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.security.Jwt.JwtUtil;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.AuthChallengePurpose;
import co.tz.sheriaconnectapi.model.Enums.AuthState;
import co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus;
import co.tz.sheriaconnectapi.repositories.StaffInvitationRepository;
import co.tz.sheriaconnectapi.repositories.StaffMfaCredentialRepository;
import co.tz.sheriaconnectapi.security.Access.AccessContextResolver;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.services.AccessManagementServices.InvitationTokenService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpHeaders;
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
import java.time.Instant;

@Service
public class LoginService implements Command<LoginInput, LoginResponse> {
    private static final int WEB_REFRESH_COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private final AuthenticationManager manager;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final WebPortalAccessService webPortalAccessService;
    private final AccessContextResolver accessContextResolver;
    private final ScopedAuthorityService scopedAuthorityService;
    private final SessionTokenIssuer sessionTokenIssuer;
    private final AuthChallengeService authChallengeService;
    private final StaffMfaCredentialRepository mfaCredentialRepository;
    private final StaffInvitationRepository invitationRepository;
    private final InvitationTokenService invitationTokenService;
    private final ProductAccessEnrollmentService productAccessEnrollmentService;

    public LoginService(
            AuthenticationManager manager,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCookieService refreshTokenCookieService,
            WebPortalAccessService webPortalAccessService,
            AccessContextResolver accessContextResolver,
            ScopedAuthorityService scopedAuthorityService,
            SessionTokenIssuer sessionTokenIssuer,
            AuthChallengeService authChallengeService,
            StaffMfaCredentialRepository mfaCredentialRepository,
            StaffInvitationRepository invitationRepository,
            InvitationTokenService invitationTokenService,
            ProductAccessEnrollmentService productAccessEnrollmentService
    ) {
        this.manager = manager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenCookieService = refreshTokenCookieService;
        this.webPortalAccessService = webPortalAccessService;
        this.accessContextResolver = accessContextResolver;
        this.scopedAuthorityService = scopedAuthorityService;
        this.sessionTokenIssuer = sessionTokenIssuer;
        this.authChallengeService = authChallengeService;
        this.mfaCredentialRepository = mfaCredentialRepository;
        this.invitationRepository = invitationRepository;
        this.invitationTokenService = invitationTokenService;
        this.productAccessEnrollmentService = productAccessEnrollmentService;
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
            throw ex;
        } catch (BadCredentialsException | InternalAuthenticationServiceException ex) {
            Throwable cause = ex.getCause();

            if (cause instanceof EmailNotVerifiedException emailNotVerifiedException) {
                throw emailNotVerifiedException;
            }

            throw new InvalidLoginCredentialsException();
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User userEntity = userRepository.findByEmail(
                loginInput.getUserLoginDTO().getEmail()
        ).orElseThrow(UserNotFoundException::new);
        ClientType clientType = accessContextResolver.clientType(loginInput.getRequest());
        AccessContext context = accessContextResolver.context(
                loginInput.getRequest(),
                clientType,
                userEntity
        );

        userEntity.setLastLoginAt(Instant.now());
        userRepository.save(userEntity);

        if (clientType == ClientType.WEB) {
            String invitationToken = loginInput.getUserLoginDTO().getInvitationToken();
            if (invitationToken != null && !invitationToken.isBlank()) {
                var invitation = invitationRepository
                        .findByTokenHash(invitationTokenService.hash(invitationToken.trim()))
                        .filter(item -> item.getStatus() == StaffInvitationStatus.PENDING)
                        .filter(item -> item.getExpiresAt().isAfter(Instant.now()))
                        .filter(item -> item.getEmail().equalsIgnoreCase(userEntity.getEmail()))
                        .orElseThrow(WebPortalAccessDeniedException::new);
                String challenge = authChallengeService.create(
                        userEntity,
                        AuthChallengePurpose.INVITATION_ACCEPTANCE,
                        invitation
                );
                return ResponseUtil.success(
                        new LoginResponse(
                                null,
                                new UserDTO(userEntity),
                                AuthState.INVITATION_ACCEPTANCE_REQUIRED,
                                challenge
                        ),
                        "Confirm the staff access invitation",
                        HttpStatus.OK
                );
            }

            if (!webPortalAccessService.canAccessWebPortal(userEntity)) {
                SecurityContextHolder.clearContext();
                throw new WebPortalAccessDeniedException();
            }
            boolean mfaEnabled = mfaCredentialRepository.findByUser(userEntity)
                    .map(credential -> credential.isEnabled())
                    .orElse(false);
            AuthChallengePurpose purpose = mfaEnabled
                    ? AuthChallengePurpose.MFA_VERIFY
                    : AuthChallengePurpose.MFA_SETUP;
            String challenge = authChallengeService.create(userEntity, purpose, null);
            return ResponseUtil.success(
                    new LoginResponse(
                            null,
                            new UserDTO(userEntity, scopedAuthorityService.require(userEntity, context)),
                            mfaEnabled ? AuthState.MFA_REQUIRED : AuthState.MFA_SETUP_REQUIRED,
                            challenge
                    ),
                    mfaEnabled
                            ? "Enter your authenticator code"
                            : "Set up multi-factor authentication",
                    HttpStatus.OK
            );
        }

        productAccessEnrollmentService.ensure(userEntity, context);
        IssuedSession issued = sessionTokenIssuer.issue(
                userEntity,
                context,
                clientType,
                false,
                loginInput.getRequest()
        );
        MobileLoginResponse mobileBody = new MobileLoginResponse(
                issued.accessToken(),
                issued.user(),
                issued.refreshToken()
        );

        return ResponseUtil.success(
                mobileBody,
                "Login successful, you're using mobile!",
                HttpStatus.OK
        );
    }
}
