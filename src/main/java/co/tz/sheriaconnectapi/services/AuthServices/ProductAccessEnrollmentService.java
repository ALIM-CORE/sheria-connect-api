package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
@Service
public class ProductAccessEnrollmentService {
    private final UserRoleAssignmentRepository assignmentRepository;
    private final RoleRepository roleRepository;

    public ProductAccessEnrollmentService(
            UserRoleAssignmentRepository assignmentRepository,
            RoleRepository roleRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void ensure(User user, AccessContext context) {
        if (context == AccessContext.STAFF
                || !assignmentRepository.findActive(user, context, Instant.now()).isEmpty()) {
            return;
        }
        String roleName = context == AccessContext.PROVIDER ? "PROVIDER" : "CITIZEN";
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AccessManagementException(
                        roleName + " role is not configured",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));
        Instant now = Instant.now();
        var existing = assignmentRepository
                .findFirstByUserAndRole_IdAndContextOrderByCreatedAtDesc(
                        user,
                        role.getId(),
                        context
                );
        if (existing.isPresent()) {
            UserRoleAssignment assignment = existing.get();
            if (assignment.getStatus() == RoleAssignmentStatus.SUSPENDED
                    || assignment.getStatus() == RoleAssignmentStatus.REVOKED) {
                throw new AccessManagementException(
                        context.name() + " access is not available",
                        HttpStatus.FORBIDDEN
                );
            }
            boolean expired = assignment.getStatus() == RoleAssignmentStatus.EXPIRED
                    || (assignment.getExpiresAt() != null
                    && !assignment.getExpiresAt().isAfter(now));
            if (!expired) {
                assignment.setStatus(RoleAssignmentStatus.ACTIVE);
                assignment.setActivatedAt(now);
                assignmentRepository.save(assignment);
                return;
            }
            assignment.setStatus(RoleAssignmentStatus.EXPIRED);
            assignment.setUpdatedAt(now);
            assignmentRepository.save(assignment);
        }

        UserRoleAssignment assignment = new UserRoleAssignment();
        assignment.setUser(user);
        assignment.setRole(role);
        assignment.setContext(context);
        assignment.setStatus(RoleAssignmentStatus.ACTIVE);
        assignment.setActivatedAt(now);
        assignment.setReason("Self-enrolled through " + context.name() + " application");
        assignmentRepository.save(assignment);
    }
}
