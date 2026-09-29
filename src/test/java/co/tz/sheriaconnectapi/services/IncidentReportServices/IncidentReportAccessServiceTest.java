package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
import co.tz.sheriaconnectapi.exceptions.InvalidTrackingTokenException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentReportAccessServiceTest {

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private TrackingTokenService trackingTokenService;
    @Mock
    private UserRoleAssignmentRepository assignmentRepository;
    @Mock
    private Authentication authentication;

    @Test
    void normalStaffSelfReviewGetsSpecificMessage() {
        User user = user(10L, "staff@example.com");
        IncidentReport report = new IncidentReport();
        report.setReporterUser(user);

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(assignmentRepository.findActive(eq(user), eq(AccessContext.STAFF), any(Instant.class)))
                .thenReturn(List.of(activeRole("CASE_MANAGER")));

        UnauthorizedCaseAccessException exception = assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> service().assertStaffNotSelf(report, authentication)
        );

        assertEquals(ErrorMessages.STAFF_SELF_REVIEW_DENIED.getMessage(), exception.getMessage());
    }

    @Test
    void superAdminCanReviewOwnCitizenReport() {
        User user = user(11L, "super@example.com");
        IncidentReport report = new IncidentReport();
        report.setReporterUser(user);

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(assignmentRepository.findActive(eq(user), eq(AccessContext.STAFF), any(Instant.class)))
                .thenReturn(List.of(activeRole("SUPER_ADMIN")));

        assertDoesNotThrow(() -> service().assertStaffNotSelf(report, authentication));
    }

    @Test
    void guestWithMatchingTrackingTokenCanAccessReport() {
        IncidentReport report = guestReport();
        when(authenticatedUserResolver.authenticatedUser(null)).thenReturn(Optional.empty());
        when(trackingTokenService.matches("valid-token", "stored-hash")).thenReturn(true);

        assertDoesNotThrow(() -> service().assertCitizenAccess(
                report,
                null,
                "valid-token"
        ));
    }

    @Test
    void guestWithoutTrackingTokenIsDenied() {
        IncidentReport report = guestReport();
        when(authenticatedUserResolver.authenticatedUser(null)).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> service().assertCitizenAccess(report, null, null)
        );
    }

    @Test
    void guestWithWrongTrackingTokenIsDenied() {
        IncidentReport report = guestReport();
        when(authenticatedUserResolver.authenticatedUser(null)).thenReturn(Optional.empty());
        when(trackingTokenService.matches("wrong-token", "stored-hash")).thenReturn(false);

        assertThrows(
                InvalidTrackingTokenException.class,
                () -> service().assertCitizenAccess(report, null, "wrong-token")
        );
    }

    @Test
    void signedInOwnerCanAccessWithoutTrackingToken() {
        User owner = user(41L, "citizen@example.com");
        IncidentReport report = new IncidentReport();
        report.setReporterUser(owner);
        when(authenticatedUserResolver.authenticatedUser(null))
                .thenReturn(Optional.of(owner));
        when(authenticatedUserResolver.requireCitizenUser(null)).thenReturn(owner);

        assertDoesNotThrow(() -> service().assertCitizenAccess(report, null, null));
    }

    @Test
    void signedInNonOwnerCannotAccessWithoutTrackingToken() {
        User owner = user(41L, "owner@example.com");
        User otherCitizen = user(42L, "other@example.com");
        IncidentReport report = new IncidentReport();
        report.setReporterUser(owner);
        when(authenticatedUserResolver.authenticatedUser(null))
                .thenReturn(Optional.of(otherCitizen));
        when(authenticatedUserResolver.requireCitizenUser(null)).thenReturn(otherCitizen);

        assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> service().assertCitizenAccess(report, null, null)
        );
    }

    private IncidentReportAccessService service() {
        return new IncidentReportAccessService(
                authenticatedUserResolver,
                trackingTokenService,
                assignmentRepository
        );
    }

    private IncidentReport guestReport() {
        IncidentReport report = new IncidentReport();
        report.setTrackingTokenHash("stored-hash");
        return report;
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    private UserRoleAssignment activeRole(String roleName) {
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setRole(new Role(roleName));
        return assignment;
    }
}
