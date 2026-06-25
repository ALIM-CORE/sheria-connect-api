package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.InvalidTokenException;
import co.tz.sheriaconnectapi.model.Entities.AuthChallenge;
import co.tz.sheriaconnectapi.model.Entities.StaffInvitation;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AuthChallengePurpose;
import co.tz.sheriaconnectapi.repositories.AuthChallengeRepository;
import co.tz.sheriaconnectapi.services.AccessManagementServices.InvitationTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthChallengeService {
    private final AuthChallengeRepository repository;
    private final InvitationTokenService tokenService;
    private final int challengeMinutes;
    private final int maxAttempts;

    public AuthChallengeService(
            AuthChallengeRepository repository,
            InvitationTokenService tokenService,
            @Value("${app.auth.mfa.challenge-minutes}") int challengeMinutes,
            @Value("${app.auth.mfa.max-attempts}") int maxAttempts
    ) {
        this.repository = repository;
        this.tokenService = tokenService;
        this.challengeMinutes = challengeMinutes;
        this.maxAttempts = maxAttempts;
    }

    public String create(User user, AuthChallengePurpose purpose, StaffInvitation invitation) {
        String rawToken = tokenService.generate();
        AuthChallenge challenge = new AuthChallenge();
        challenge.setTokenHash(tokenService.hash(rawToken));
        challenge.setUser(user);
        challenge.setPurpose(purpose);
        challenge.setStaffInvitation(invitation);
        challenge.setExpiresAt(Instant.now().plus(challengeMinutes, ChronoUnit.MINUTES));
        repository.save(challenge);
        return rawToken;
    }

    @Transactional
    public AuthChallenge require(String rawToken, AuthChallengePurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidTokenException("Authentication challenge is required");
        }
        AuthChallenge challenge = repository.findByTokenHash(tokenService.hash(rawToken.trim()))
                .orElseThrow(() -> new InvalidTokenException("Authentication challenge is invalid"));
        if (challenge.isConsumed()
                || challenge.getPurpose() != purpose
                || challenge.getExpiresAt().isBefore(Instant.now())
                || challenge.getAttempts() >= maxAttempts) {
            throw new InvalidTokenException("Authentication challenge is invalid or expired");
        }
        return challenge;
    }

    @Transactional
    public void failed(AuthChallenge challenge) {
        challenge.setAttempts(challenge.getAttempts() + 1);
        repository.save(challenge);
    }

    @Transactional
    public void consume(AuthChallenge challenge) {
        challenge.setConsumed(true);
        repository.save(challenge);
    }
}
