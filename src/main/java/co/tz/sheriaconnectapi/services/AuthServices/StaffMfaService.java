package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.MfaCodeRequest;
import co.tz.sheriaconnectapi.model.DTOs.MfaSetupResponse;
import co.tz.sheriaconnectapi.model.Entities.MfaRecoveryCode;
import co.tz.sheriaconnectapi.model.Entities.StaffMfaCredential;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.AuthChallengePurpose;
import co.tz.sheriaconnectapi.model.Enums.AuthState;
import co.tz.sheriaconnectapi.repositories.MfaRecoveryCodeRepository;
import co.tz.sheriaconnectapi.repositories.StaffMfaCredentialRepository;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import co.tz.sheriaconnectapi.services.AccessManagementServices.InvitationTokenService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

@Service
public class StaffMfaService {
    private static final Duration WEB_REFRESH_DURATION = Duration.ofDays(7);

    private final AuthChallengeService challengeService;
    private final StaffMfaCredentialRepository credentialRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final MfaSecretCipher cipher;
    private final TotpService totpService;
    private final InvitationTokenService tokenService;
    private final SessionTokenIssuer sessionTokenIssuer;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public StaffMfaService(
            AuthChallengeService challengeService,
            StaffMfaCredentialRepository credentialRepository,
            MfaRecoveryCodeRepository recoveryCodeRepository,
            MfaSecretCipher cipher,
            TotpService totpService,
            InvitationTokenService tokenService,
            SessionTokenIssuer sessionTokenIssuer,
            RefreshTokenCookieService refreshTokenCookieService
    ) {
        this.challengeService = challengeService;
        this.credentialRepository = credentialRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.cipher = cipher;
        this.totpService = totpService;
        this.tokenService = tokenService;
        this.sessionTokenIssuer = sessionTokenIssuer;
        this.refreshTokenCookieService = refreshTokenCookieService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<MfaSetupResponse>> setup(String challengeToken) {
        var challenge = challengeService.require(
                challengeToken,
                AuthChallengePurpose.MFA_SETUP
        );
        String secret = totpService.generateSecret();
        StaffMfaCredential credential = credentialRepository.findByUser(challenge.getUser())
                .orElseGet(StaffMfaCredential::new);
        credential.setUser(challenge.getUser());
        credential.setEncryptedSecret(cipher.encrypt(secret));
        credential.setEnabled(false);
        credential.setConfirmedAt(null);
        credentialRepository.save(credential);

        return ResponseUtil.success(
                new MfaSetupResponse(
                        secret,
                        totpService.provisioningUri(challenge.getUser().getEmail(), secret)
                ),
                "Authenticator setup created",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<LoginResponse>> confirmSetup(
            MfaCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        var challenge = challengeService.require(
                request == null ? null : request.challengeToken(),
                AuthChallengePurpose.MFA_SETUP
        );
        StaffMfaCredential credential = credentialRepository.findByUser(challenge.getUser())
                .orElseThrow(() -> new InvalidTokenException("MFA setup was not started"));
        String secret = cipher.decrypt(credential.getEncryptedSecret());
        OptionalLong verifiedCounter = totpService.verifiedCounter(secret, request.code());
        if (verifiedCounter.isEmpty()) {
            challengeService.failed(challenge);
            throw new InvalidTokenException("Invalid authenticator code");
        }

        credential.setEnabled(true);
        credential.setConfirmedAt(Instant.now());
        credential.setLastUsedTotpCounter(verifiedCounter.getAsLong());
        credentialRepository.save(credential);
        recoveryCodeRepository.deleteAllByCredential(credential);

        List<String> recoveryCodes = totpService.recoveryCodes();
        for (String rawCode : recoveryCodes) {
            MfaRecoveryCode recoveryCode = new MfaRecoveryCode();
            recoveryCode.setCredential(credential);
            recoveryCode.setCodeHash(tokenService.hash(normalizeRecoveryCode(rawCode)));
            recoveryCodeRepository.save(recoveryCode);
        }
        challengeService.consume(challenge);
        return authenticatedResponse(challenge.getUser(), servletRequest, recoveryCodes);
    }

    @Transactional
    public ResponseEntity<StandardResponse<LoginResponse>> verify(
            MfaCodeRequest request,
            HttpServletRequest servletRequest
    ) {
        var challenge = challengeService.require(
                request == null ? null : request.challengeToken(),
                AuthChallengePurpose.MFA_VERIFY
        );
        StaffMfaCredential credential = credentialRepository.findByUser(challenge.getUser())
                .filter(StaffMfaCredential::isEnabled)
                .orElseThrow(() -> new InvalidTokenException("MFA is not configured"));

        OptionalLong verifiedCounter = totpService.verifiedCounter(
                cipher.decrypt(credential.getEncryptedSecret()),
                request.code()
        );
        boolean valid = verifiedCounter.isPresent()
                && (credential.getLastUsedTotpCounter() == null
                || verifiedCounter.getAsLong() > credential.getLastUsedTotpCounter());
        if (valid) {
            credential.setLastUsedTotpCounter(verifiedCounter.getAsLong());
            credentialRepository.save(credential);
        }
        if (!valid && request.recoveryCode() != null) {
            String hash = tokenService.hash(normalizeRecoveryCode(request.recoveryCode()));
            var matchingCode = recoveryCodeRepository
                    .findAllByCredentialAndUsedAtIsNull(credential)
                    .stream()
                    .filter(code -> code.getCodeHash().equals(hash))
                    .findFirst();
            if (matchingCode.isPresent()) {
                matchingCode.get().setUsedAt(Instant.now());
                recoveryCodeRepository.save(matchingCode.get());
                valid = true;
            }
        }

        if (!valid) {
            challengeService.failed(challenge);
            throw new InvalidTokenException("Invalid authenticator or recovery code");
        }
        challengeService.consume(challenge);
        return authenticatedResponse(challenge.getUser(), servletRequest, List.of());
    }

    private ResponseEntity<StandardResponse<LoginResponse>> authenticatedResponse(
            co.tz.sheriaconnectapi.model.Entities.User user,
            HttpServletRequest request,
            List<String> recoveryCodes
    ) {
        IssuedSession issued = sessionTokenIssuer.issue(
                user,
                AccessContext.STAFF,
                ClientType.WEB,
                true,
                request
        );
        LoginResponse body = new LoginResponse(
                issued.accessToken(),
                issued.user(),
                AuthState.AUTHENTICATED,
                null,
                recoveryCodes
        );
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieService.create(
                                issued.refreshToken(),
                                WEB_REFRESH_DURATION
                        )
                )
                .body(new StandardResponse<>(true, "Login successful", body, null));
    }

    private String normalizeRecoveryCode(String value) {
        return value == null ? "" : value.replace("-", "").trim().toUpperCase();
    }
}
