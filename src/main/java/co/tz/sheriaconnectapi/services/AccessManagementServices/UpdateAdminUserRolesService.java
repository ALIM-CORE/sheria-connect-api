package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateUserRolesRequest;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UpdateAdminUserRolesService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AuthSessionService authSessionService;
    private final AccessManagementPolicy policy;
    private final AdminUserResponseFactory responseFactory;
    private final AccessAuditService auditService;

    public UpdateAdminUserRolesService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleAssignmentRepository assignmentRepository,
            StaffProfileRepository staffProfileRepository,
            AuthSessionService authSessionService,
            AccessManagementPolicy policy,
            AdminUserResponseFactory responseFactory,
            AccessAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.authSessionService = authSessionService;
        this.policy = policy;
        this.responseFactory = responseFactory;
        this.auditService = auditService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> execute(
            Long userId,
            UpdateUserRolesRequest request,
            Authentication authentication
    ) {
        if (request == null || request.roleIds() == null || request.roleIds().isEmpty()) {
            throw new AccessManagementException("At least one role is required", HttpStatus.BAD_REQUEST);
        }
        if (request.reason() == null || request.reason().isBlank()) {
            throw new AccessManagementException("A reason is required", HttpStatus.BAD_REQUEST);
        }

        var actor = policy.requireActor(authentication);
        var target = userRepository.findDetailedById(userId).orElseThrow(UserNotFoundException::new);
        if (staffProfileRepository.findByUser(target).isEmpty()) {
            throw new AccessManagementException("This account does not have staff access", HttpStatus.BAD_REQUEST);
        }
        Set<Role> requestedRoles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
        if (requestedRoles.size() != request.roleIds().size()) {
            throw new AccessManagementException("One or more roles do not exist", HttpStatus.BAD_REQUEST);
        }
        policy.requireCanAssignStaffRoles(actor, target, requestedRoles);
        policy.ensureLastSuperAdminRemainsUsable(
                target,
                requestedRoles,
                true,
                false
        );

        var current = assignmentRepository.findActive(target, AccessContext.STAFF, Instant.now());
        Set<Long> requestedRoleIds = requestedRoles.stream().map(Role::getId).collect(Collectors.toSet());
        for (UserRoleAssignment assignment : current) {
            if (!requestedRoleIds.contains(assignment.getRole().getId())) {
                assignment.setStatus(RoleAssignmentStatus.REVOKED);
                assignment.setChangedByUser(actor);
                assignment.setRevokedAt(Instant.now());
                assignment.setReason(request.reason().trim());
                assignmentRepository.save(assignment);
            }
        }

        Set<Long> currentRoleIds = current.stream()
                .filter(item -> item.getStatus() == RoleAssignmentStatus.ACTIVE)
                .map(item -> item.getRole().getId())
                .collect(Collectors.toSet());
        for (Role role : requestedRoles) {
            if (currentRoleIds.contains(role.getId())) {
                continue;
            }
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(target);
            assignment.setRole(role);
            assignment.setContext(AccessContext.STAFF);
            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
            assignment.setGrantedByUser(actor);
            assignment.setActivatedAt(Instant.now());
            assignment.setReason(request.reason().trim());
            assignmentRepository.save(assignment);
        }

        authSessionService.revokeContext(target.getId(), AccessContext.STAFF);
        auditService.log(
                actor,
                target,
                "STAFF_ROLES_UPDATED",
                "USER",
                target.getId(),
                request.reason().trim()
        );
        return ResponseUtil.success(
                responseFactory.create(target, policy.canViewLinkedIdentity(authentication)),
                "Staff roles updated",
                HttpStatus.OK
        );
    }
}
