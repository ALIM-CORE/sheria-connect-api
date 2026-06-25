package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAccessEnrollmentServiceTest {
    @Mock
    private UserRoleAssignmentRepository assignmentRepository;
    @Mock
    private RoleRepository roleRepository;

    private ProductAccessEnrollmentService service;

    @BeforeEach
    void setUp() {
        service = new ProductAccessEnrollmentService(assignmentRepository, roleRepository);
    }

    @Test
    void createsProviderParticipationForAnIdentityWithoutIt() {
        User user = new User();
        user.setId(7L);
        Role provider = new Role();
        provider.setId(3L);
        provider.setName("PROVIDER");

        when(assignmentRepository.findActive(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(AccessContext.PROVIDER),
                any(Instant.class)
        )).thenReturn(List.of());
        when(roleRepository.findByName("PROVIDER")).thenReturn(Optional.of(provider));
        when(assignmentRepository.findFirstByUserAndRole_IdAndContextOrderByCreatedAtDesc(
                user,
                3L,
                AccessContext.PROVIDER
        )).thenReturn(Optional.empty());

        service.ensure(user, AccessContext.PROVIDER);

        ArgumentCaptor<UserRoleAssignment> captor =
                ArgumentCaptor.forClass(UserRoleAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertEquals(RoleAssignmentStatus.ACTIVE, captor.getValue().getStatus());
        assertEquals(AccessContext.PROVIDER, captor.getValue().getContext());
    }

    @Test
    void doesNotAllowARevokedCapabilityToSelfReactivate() {
        User user = new User();
        Role citizen = new Role();
        citizen.setId(2L);
        citizen.setName("CITIZEN");
        UserRoleAssignment revoked = new UserRoleAssignment();
        revoked.setStatus(RoleAssignmentStatus.REVOKED);

        when(assignmentRepository.findActive(
                org.mockito.ArgumentMatchers.eq(user),
                org.mockito.ArgumentMatchers.eq(AccessContext.CITIZEN),
                any(Instant.class)
        )).thenReturn(List.of());
        when(roleRepository.findByName("CITIZEN")).thenReturn(Optional.of(citizen));
        when(assignmentRepository.findFirstByUserAndRole_IdAndContextOrderByCreatedAtDesc(
                user,
                2L,
                AccessContext.CITIZEN
        )).thenReturn(Optional.of(revoked));

        assertThrows(
                AccessManagementException.class,
                () -> service.ensure(user, AccessContext.CITIZEN)
        );
        verify(assignmentRepository, never()).save(any());
    }
}
