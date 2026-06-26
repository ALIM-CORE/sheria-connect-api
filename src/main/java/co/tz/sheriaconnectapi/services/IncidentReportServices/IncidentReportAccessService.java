package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.InvalidTrackingTokenException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class IncidentReportAccessService {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TrackingTokenService trackingTokenService;

    public IncidentReportAccessService(
            AuthenticatedUserResolver authenticatedUserResolver,
            TrackingTokenService trackingTokenService
    ) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.trackingTokenService = trackingTokenService;
    }

    public Optional<User> authenticatedUser(Authentication authentication) {
        return authenticatedUserResolver.authenticatedUser(authentication);
    }

    public User requireCitizenUser(Authentication authentication) {
        return authenticatedUserResolver.requireCitizenUser(authentication);
    }

    public Optional<User> authenticatedCitizenUser(Authentication authentication) {
        Optional<User> user = authenticatedUserResolver.authenticatedUser(authentication);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        authenticatedUserResolver.requireCitizenUser(authentication);
        return user;
    }

    public void assertCitizenAccess(
            IncidentReport report,
            Authentication authentication,
            String trackingToken
    ) {
        Optional<User> authenticatedUser = authenticatedCitizenUser(authentication);

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

    public void assertStaffNotSelf(
            IncidentReport report,
            Authentication authentication
    ) {
        Optional<User> actor = authenticatedUser(authentication);
        if (actor.isPresent()
                && report.getReporterUser() != null
                && report.getReporterUser().getId().equals(actor.get().getId())) {
            throw new UnauthorizedCaseAccessException();
        }
    }
}
