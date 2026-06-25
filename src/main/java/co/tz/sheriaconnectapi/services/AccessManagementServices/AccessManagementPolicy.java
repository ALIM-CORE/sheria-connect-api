package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service("accessManagementPolicy")
public class AccessManagementPolicy {
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String SUPPORT_OFFICER = "SUPPORT_OFFICER";

    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final ScopedAuthorityService authorityService;
    private final StaffProfileRepository staffProfileRepository;

    public AccessManagementPolicy(
            UserRepository userRepository,
            UserRoleAssignmentRepository assignmentRepository,
            ScopedAuthorityService authorityService,
            StaffProfileRepository staffProfileRepository
    ) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.authorityService = authorityService;
        this.staffProfileRepository = staffProfileRepository;
    }

    public boolean isSuperAdmin(Authentication authentication) {
        return authenticatedUser(authentication)
                .map(user -> hasRole(user, SUPER_ADMIN))
                .orElse(false);
    }

    public boolean canViewLinkedIdentity(Authentication authentication) {
        return authenticatedUser(authentication)
                .map(user -> hasRole(user, SUPER_ADMIN) || hasRole(user, SYSTEM_ADMIN))
                .orElse(false);
    }

    public User requireActor(Authentication authentication) {
        return authenticatedUser(authentication)
                .orElseThrow(() -> new AccessManagementException(
                        "An authenticated staff account is required",
                        HttpStatus.UNAUTHORIZED
                ));
    }

    public void requireCanManageStatus(User actor, User target) {
        if (hasRole(actor, SUPER_ADMIN)) {
            return;
        }

        if (hasRole(actor, SYSTEM_ADMIN)) {
            if (!hasRole(target, SUPER_ADMIN) && !hasRole(target, SYSTEM_ADMIN)) {
                return;
            }
        }

        if (hasRole(actor, SUPPORT_OFFICER)
                && staffProfileRepository.findByUser(target).isEmpty()
                && !hasRole(target, SUPER_ADMIN)
                && !hasRole(target, SYSTEM_ADMIN)) {
            return;
        }

        throw forbidden();
    }

    public void requireCanAssignStaffRoles(User actor, User target, Set<Role> roles) {
        if (actor.getId().equals(target.getId())) {
            throw new AccessManagementException(
                    "You cannot change your own staff roles",
                    HttpStatus.FORBIDDEN
            );
        }

        if (roles.isEmpty() || roles.stream().anyMatch(role ->
                role.getAudience() != RoleAudience.PLATFORM_STAFF)) {
            throw new AccessManagementException(
                    "At least one platform staff role is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (hasRole(actor, SUPER_ADMIN)) {
            return;
        }

        if (hasRole(actor, SYSTEM_ADMIN)
                && roles.stream().noneMatch(role ->
                Set.of(SUPER_ADMIN, SYSTEM_ADMIN).contains(role.getName()))
                && !hasRole(target, SUPER_ADMIN)
                && !hasRole(target, SYSTEM_ADMIN)) {
            return;
        }

        throw forbidden();
    }

    public void requireCanInvite(User actor, Set<Role> roles) {
        User placeholder = new User();
        placeholder.setId(-1L);
        requireCanAssignStaffRoles(actor, placeholder, roles);
    }

    public void ensureLastSuperAdminRemainsUsable(
            User target,
            Set<Role> targetRoles,
            Boolean active,
            Boolean locked
    ) {
        if (!hasRole(target, SUPER_ADMIN)) {
            return;
        }

        long count = authorityService.resolve(target, AccessContext.STAFF).roles().stream()
                .filter(role -> SUPER_ADMIN.equals(role.getName()))
                .findFirst()
                .map(role -> assignmentRepository.countUsableUsersByRoleAndContext(
                        role.getId(),
                        AccessContext.STAFF,
                        java.time.Instant.now()
                ))
                .orElse(0L);

        if (count > 1) {
            return;
        }

        boolean keepsRole = targetRoles.stream().anyMatch(role -> SUPER_ADMIN.equals(role.getName()));
        if (!keepsRole || !Boolean.TRUE.equals(active) || Boolean.TRUE.equals(locked)) {
            throw new AccessManagementException(
                    "The last Super Admin account cannot be demoted, suspended, or locked",
                    HttpStatus.CONFLICT
            );
        }
    }

    public boolean hasRole(User user, String roleName) {
        return user != null && authorityService.resolve(user, AccessContext.STAFF).roles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }

    private java.util.Optional<User> authenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    private AccessManagementException forbidden() {
        return new AccessManagementException(
                "You are not allowed to manage this account",
                HttpStatus.FORBIDDEN
        );
    }
}
