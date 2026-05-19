package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.InvalidTrackingTokenException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IncidentReportAccessService {

    private final UserRepository userRepository;
    private final TrackingTokenService trackingTokenService;

    public IncidentReportAccessService(
            UserRepository userRepository,
            TrackingTokenService trackingTokenService
    ) {
        this.userRepository = userRepository;
        this.trackingTokenService = trackingTokenService;
    }

    public Optional<User> authenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }

        return userRepository.findByEmail(authentication.getName());
    }

    public User requireAuthenticatedUser(Authentication authentication) {
        return authenticatedUser(authentication)
                .orElseThrow(UnauthorizedCaseAccessException::new);
    }

    public void assertCitizenAccess(
            IncidentReport report,
            Authentication authentication,
            String trackingToken
    ) {
        Optional<User> authenticatedUser = authenticatedUser(authentication);

        if (report.getReporterUser() != null && authenticatedUser.isPresent()) {
            String reportOwnerEmail = report.getReporterUser().getEmail();
            if (reportOwnerEmail.equalsIgnoreCase(authenticatedUser.get().getEmail())) {
                return;
            }
        }

        if (report.getTrackingTokenHash() != null) {
            if (trackingToken == null || trackingToken.isBlank()) {
                throw new UnauthorizedCaseAccessException();
            }

            if (!trackingTokenService.matches(trackingToken, report.getTrackingTokenHash())) {
                throw new InvalidTrackingTokenException();
            }

            return;
        }

        throw new UnauthorizedCaseAccessException();
    }
}
