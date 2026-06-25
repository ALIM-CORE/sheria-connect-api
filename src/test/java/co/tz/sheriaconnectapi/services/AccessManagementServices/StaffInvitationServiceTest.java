package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.model.DTOs.CreateLinkedStaffInvitationRequest;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.StaffInvitation;
import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import co.tz.sheriaconnectapi.repositories.*;
import co.tz.sheriaconnectapi.services.AuthServices.AuthChallengeService;
import co.tz.sheriaconnectapi.services.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StaffInvitationServiceTest {
    private StaffInvitationRepository invitationRepository;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private UserRoleAssignmentRepository assignmentRepository;
    private StaffProfileRepository staffProfileRepository;
    private AccessManagementPolicy policy;
    private StaffInvitationService service;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(StaffInvitationRepository.class);
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        assignmentRepository = mock(UserRoleAssignmentRepository.class);
        staffProfileRepository = mock(StaffProfileRepository.class);
        policy = mock(AccessManagementPolicy.class);
        service = new StaffInvitationService(
                invitationRepository,
                userRepository,
                roleRepository,
                assignmentRepository,
                staffProfileRepository,
                mock(StaffMfaCredentialRepository.class),
                mock(PasswordEncoder.class),
                new InvitationTokenService(),
                mock(AuthChallengeService.class),
                policy,
                mock(AccessAuditService.class),
                mock(EmailService.class)
        );
        ReflectionTestUtils.setField(service, "frontendBaseDomain", "http://sheriaconnect.co.tz");
        when(invitationRepository.save(any())).thenAnswer(invocation -> {
            StaffInvitation invitation = invocation.getArgument(0);
            invitation.setId(10L);
            return invitation;
        });
        when(staffProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void existingCitizenReceivesPendingStaffAccessOnSameIdentity() {
        User actor = user(1L, "owner@sheriaconnect.co.tz");
        User citizen = user(2L, "citizen@example.com");
        Role role = role(3L, "CASE_MANAGER");
        Authentication authentication = mock(Authentication.class);
        when(policy.requireActor(authentication)).thenReturn(actor);
        when(userRepository.findDetailedById(2L)).thenReturn(Optional.of(citizen));
        when(roleRepository.findAllById(Set.of(3L))).thenReturn(List.of(role));
        when(staffProfileRepository.findByUser(citizen)).thenReturn(Optional.empty());
        when(invitationRepository.findFirstByLinkedProductUserAndStatusOrderByCreatedAtDesc(
                eq(citizen),
                any()
        )).thenReturn(Optional.empty());

        service.createLinked(
                2L,
                request(Set.of(3L)),
                authentication
        );

        ArgumentCaptor<StaffInvitation> invitationCaptor = ArgumentCaptor.forClass(StaffInvitation.class);
        verify(invitationRepository).save(invitationCaptor.capture());
        assertEquals(citizen, invitationCaptor.getValue().getLinkedProductUser());
        assertEquals(citizen.getEmail(), invitationCaptor.getValue().getEmail());

        ArgumentCaptor<StaffProfile> profileCaptor = ArgumentCaptor.forClass(StaffProfile.class);
        verify(staffProfileRepository).save(profileCaptor.capture());
        assertEquals(citizen, profileCaptor.getValue().getUser());
        assertEquals(StaffEmploymentStatus.PENDING, profileCaptor.getValue().getEmploymentStatus());

        ArgumentCaptor<UserRoleAssignment> assignmentCaptor =
                ArgumentCaptor.forClass(UserRoleAssignment.class);
        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertEquals(citizen, assignmentCaptor.getValue().getUser());
        verify(userRepository, never()).save(any());
    }

    @Test
    void duplicateStaffProfileIsRejected() {
        User actor = user(1L, "owner@sheriaconnect.co.tz");
        User citizen = user(2L, "citizen@example.com");
        Role role = role(3L, "CASE_MANAGER");
        Authentication authentication = mock(Authentication.class);
        when(policy.requireActor(authentication)).thenReturn(actor);
        when(userRepository.findDetailedById(2L)).thenReturn(Optional.of(citizen));
        when(roleRepository.findAllById(Set.of(3L))).thenReturn(List.of(role));
        when(staffProfileRepository.findByUser(citizen)).thenReturn(Optional.of(new StaffProfile()));

        assertThrows(RuntimeException.class, () -> service.createLinked(
                2L,
                request(Set.of(3L)),
                authentication
        ));
    }

    private CreateLinkedStaffInvitationRequest request(Set<Long> roles) {
        return new CreateLinkedStaffInvitationRequest(
                roles,
                "Case Officer",
                "Case Operations",
                null,
                null,
                Instant.now().plusSeconds(86400),
                "Employment onboarding"
        );
    }

    private User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setName("Test User");
        user.setEmail(email);
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    private Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setDisplayName("Case Manager");
        role.setAudience(RoleAudience.PLATFORM_STAFF);
        return role;
    }
}
