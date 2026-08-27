package co.tz.sheriaconnectapi.services.IncidentReportServices;

import co.tz.sheriaconnectapi.exceptions.ErrorMessages;
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
        User user = user(10L);
        IncidentReport report = report(user);
        IncidentReportAccessService service = service();

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(assignmentRepository.findActive(eq(user), eq(AccessContext.STAFF), any(Instant.class)))
                .thenReturn(List.of(activeRole("CASE_MANAGER")));

        UnauthorizedCaseAccessException exception = assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> service.assertStaffNotSelf(report, authentication)
        );

        assertEquals(ErrorMessages.STAFF_SELF_REVIEW_DENIED.getMessage(), exception.getMessage());
    }

    @Test
    void superAdminCanReviewOwnCitizenReport() {
        User user = user(11L);
        IncidentReport report = report(user);
        IncidentReportAccessService service = service();

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(assignmentRepository.findActive(eq(user), eq(AccessContext.STAFF), any(Instant.class)))
                .thenReturn(List.of(activeRole("SUPER_ADMIN")));

        assertDoesNotThrow(() -> service.assertStaffNotSelf(report, authentication));
    }

    private IncidentReportAccessService service() {
        return new IncidentReportAccessService(
                authenticatedUserResolver,
                trackingTokenService,
                assignmentRepository
        );
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@sheriaconnect.co.tz");
        return user;
    }

    private IncidentReport report(User reporter) {
        IncidentReport report = new IncidentReport();
        report.setReporterUser(reporter);
        return report;
    }

    private UserRoleAssignment activeRole(String roleName) {
        Role role = new Role(roleName);
        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setRole(role);
        return assignment;
    }
}
