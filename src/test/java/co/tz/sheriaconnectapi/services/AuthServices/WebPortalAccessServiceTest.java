package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.Authority;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Access.EffectiveAccess;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebPortalAccessServiceTest {
    private ScopedAuthorityService authorityService;
    private WebPortalAccessService service;

    @BeforeEach
    void setUp() {
        authorityService = mock(ScopedAuthorityService.class);
        service = new WebPortalAccessService(authorityService);
    }

    @Test
    void identityWithoutStaffAssignmentCannotUsePortal() {
        User user = user();
        when(authorityService.resolve(user, AccessContext.STAFF))
                .thenReturn(new EffectiveAccess(AccessContext.STAFF, List.of(), Set.of()));
        assertFalse(service.canAccessWebPortal(user));
    }

    @Test
    void operationalStaffAuthorityAllowsPortal() {
        User user = user();
        Role role = role("CASE_MANAGER", "INCIDENTREPORT_READ");
        when(authorityService.resolve(user, AccessContext.STAFF))
                .thenReturn(new EffectiveAccess(AccessContext.STAFF, List.of(role), Set.of()));
        assertTrue(service.canAccessWebPortal(user));
    }

    @Test
    void globalSuspensionBlocksPortal() {
        User user = user();
        user.setActive(false);
        assertFalse(service.canAccessWebPortal(user));
    }

    private User user() {
        User user = new User();
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    private Role role(String name, String authorityName) {
        Role role = new Role();
        role.setName(name);
        Authority authority = new Authority();
        authority.setName(authorityName);
        role.setAuthorities(new HashSet<>(Set.of(authority)));
        return role;
    }
}
