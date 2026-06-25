package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccessManagementPolicyTest {
    private UserRoleAssignmentRepository assignmentRepository;
    private StaffProfileRepository staffProfileRepository;
    private ScopedAuthorityService authorityService;
    private AccessManagementPolicy policy;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        assignmentRepository = mock(UserRoleAssignmentRepository.class);
        staffProfileRepository = mock(StaffProfileRepository.class);
        authorityService = mock(ScopedAuthorityService.class);
        policy = new AccessManagementPolicy(
                userRepository,
                assignmentRepository,
                authorityService,
                staffProfileRepository
        );
    }

    @Test
    void systemAdminCanManageOperationalStaffAndProductIdentity() {
        User actor = user(1L);
        User staff = user(2L);
        User citizen = user(3L);
        roles(actor, role(2L, "SYSTEM_ADMIN", RoleAudience.PLATFORM_STAFF));
        roles(staff, role(3L, "CASE_MANAGER", RoleAudience.PLATFORM_STAFF));
        roles(citizen);

        assertDoesNotThrow(() -> policy.requireCanManageStatus(actor, staff));
        assertDoesNotThrow(() -> policy.requireCanManageStatus(actor, citizen));
    }

    @Test
    void supportOfficerCannotManageStaffProfile() {
        User actor = user(1L);
        User target = user(2L);
        roles(actor, role(6L, "SUPPORT_OFFICER", RoleAudience.PLATFORM_STAFF));
        roles(target, role(3L, "CASE_MANAGER", RoleAudience.PLATFORM_STAFF));
        when(staffProfileRepository.findByUser(target)).thenReturn(Optional.of(new StaffProfile()));

        assertThrows(AccessManagementException.class, () -> policy.requireCanManageStatus(actor, target));
    }

    @Test
    void productAndStaffRolesCanCoexistButStaffAssignmentOnlyAcceptsStaffRoles() {
        User owner = user(1L);
        User target = user(2L);
        roles(owner, role(1L, "SUPER_ADMIN", RoleAudience.PLATFORM_STAFF));
        roles(target);

        assertThrows(
                AccessManagementException.class,
                () -> policy.requireCanAssignStaffRoles(
                        owner,
                        target,
                        Set.of(role(7L, "CITIZEN", RoleAudience.PRODUCT_IDENTITY))
                )
        );
        assertDoesNotThrow(
                () -> policy.requireCanAssignStaffRoles(
                        owner,
                        target,
                        Set.of(role(3L, "CASE_MANAGER", RoleAudience.PLATFORM_STAFF))
                )
        );
    }

    @Test
    void lastSuperAdminCannotBeRemoved() {
        Role superRole = role(1L, "SUPER_ADMIN", RoleAudience.PLATFORM_STAFF);
        User target = user(2L);
        roles(target, superRole);
        when(assignmentRepository.countUsableUsersByRoleAndContext(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(AccessContext.STAFF),
                org.mockito.ArgumentMatchers.any(java.time.Instant.class)
        )).thenReturn(1L);

        assertThrows(
                AccessManagementException.class,
                () -> policy.ensureLastSuperAdminRemainsUsable(
                        target,
                        Set.of(role(3L, "CASE_MANAGER", RoleAudience.PLATFORM_STAFF)),
                        true,
                        false
                )
        );
    }

    private void roles(User user, Role... roles) {
        when(authorityService.resolve(user, AccessContext.STAFF))
                .thenReturn(new EffectiveAccess(AccessContext.STAFF, List.of(roles), new java.util.HashSet<>()));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    private Role role(Long id, String name, RoleAudience audience) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDisplayName(name);
        role.setAudience(audience);
        return role;
    }
}
