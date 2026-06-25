package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateStaffAccessRequest;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.services.AuthServices.AuthSessionService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class UpdateStaffAccessService {
    private final UserRepository userRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AuthSessionService authSessionService;
    private final AccessManagementPolicy policy;
    private final AdminUserResponseFactory responseFactory;
    private final AccessAuditService auditService;

    public UpdateStaffAccessService(
            UserRepository userRepository,
            StaffProfileRepository staffProfileRepository,
            UserRoleAssignmentRepository assignmentRepository,
            AuthSessionService authSessionService,
            AccessManagementPolicy policy,
            AdminUserResponseFactory responseFactory,
            AccessAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.assignmentRepository = assignmentRepository;
        this.authSessionService = authSessionService;
        this.policy = policy;
        this.responseFactory = responseFactory;
        this.auditService = auditService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> execute(
            Long userId,
            UpdateStaffAccessRequest request,
            Authentication authentication
    ) {
        if (request == null || request.action() == null || request.reason() == null
                || request.reason().isBlank()) {
            throw new AccessManagementException("Action and reason are required", HttpStatus.BAD_REQUEST);
        }
        var actor = policy.requireActor(authentication);
        var target = userRepository.findDetailedById(userId).orElseThrow(UserNotFoundException::new);
        if (actor.getId().equals(target.getId())) {
            throw new AccessManagementException(
                    "You cannot change your own staff access",
                    HttpStatus.FORBIDDEN
            );
        }
        policy.requireCanManageStatus(actor, target);
        var profile = staffProfileRepository.findByUser(target)
                .orElseThrow(() -> new AccessManagementException(
                        "This account does not have staff access",
                        HttpStatus.BAD_REQUEST
                ));

        String action = request.action().trim().toUpperCase(Locale.ROOT);
        var activeAssignments = assignmentRepository.findActive(
                target,
                AccessContext.STAFF,
                Instant.now()
        );
        switch (action) {
            case "SUSPEND" -> {
                policy.ensureLastSuperAdminRemainsUsable(target, java.util.Set.of(), false, true);
                profile.setEmploymentStatus(StaffEmploymentStatus.SUSPENDED);
                profile.setSuspendedAt(Instant.now());
                activeAssignments.forEach(assignment -> {
                    assignment.setStatus(RoleAssignmentStatus.SUSPENDED);
                    assignment.setChangedByUser(actor);
                    assignment.setSuspendedAt(Instant.now());
                    assignment.setReason(request.reason().trim());
                    assignmentRepository.save(assignment);
                });
            }
            case "RESTORE" -> {
                profile.setEmploymentStatus(StaffEmploymentStatus.ACTIVE);
                profile.setSuspendedAt(null);
                assignmentRepository.findAllByUserAndContextOrderByCreatedAtDesc(
                        target,
                        AccessContext.STAFF
                ).stream()
                        .filter(item -> item.getStatus() == RoleAssignmentStatus.SUSPENDED)
                        .forEach(assignment -> {
                            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
                            assignment.setChangedByUser(actor);
                            assignment.setActivatedAt(Instant.now());
                            assignment.setReason(request.reason().trim());
                            assignmentRepository.save(assignment);
                        });
            }
            case "REVOKE" -> {
                policy.ensureLastSuperAdminRemainsUsable(target, java.util.Set.of(), false, true);
                profile.setEmploymentStatus(StaffEmploymentStatus.REVOKED);
                activeAssignments.forEach(assignment -> {
                    assignment.setStatus(RoleAssignmentStatus.REVOKED);
                    assignment.setChangedByUser(actor);
                    assignment.setRevokedAt(Instant.now());
                    assignment.setReason(request.reason().trim());
                    assignmentRepository.save(assignment);
                });
            }
            default -> throw new AccessManagementException(
                    "Action must be SUSPEND, RESTORE, or REVOKE",
                    HttpStatus.BAD_REQUEST
            );
        }
        staffProfileRepository.save(profile);
        authSessionService.revokeContext(target.getId(), AccessContext.STAFF);
        auditService.log(
                actor,
                target,
                "STAFF_ACCESS_" + action,
                "STAFF_PROFILE",
                profile.getId(),
                request.reason().trim()
        );
        return ResponseUtil.success(
                responseFactory.create(target, policy.canViewLinkedIdentity(authentication)),
                "Staff access updated",
                HttpStatus.OK
        );
    }
}
