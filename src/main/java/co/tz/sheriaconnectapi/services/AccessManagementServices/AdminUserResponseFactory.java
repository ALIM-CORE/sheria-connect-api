package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.model.DTOs.RoleSummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.ProviderProfile;
import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import co.tz.sheriaconnectapi.repositories.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AdminUserResponseFactory {
    private final IncidentReportRepository incidentReportRepository;
    private final PublicStoryRepository publicStoryRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final StaffInvitationRepository invitationRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffMfaCredentialRepository mfaCredentialRepository;

    public AdminUserResponseFactory(
            IncidentReportRepository incidentReportRepository,
            PublicStoryRepository publicStoryRepository,
            ProviderProfileRepository providerProfileRepository,
            StaffInvitationRepository invitationRepository,
            UserRoleAssignmentRepository assignmentRepository,
            StaffProfileRepository staffProfileRepository,
            StaffMfaCredentialRepository mfaCredentialRepository
    ) {
        this.incidentReportRepository = incidentReportRepository;
        this.publicStoryRepository = publicStoryRepository;
        this.providerProfileRepository = providerProfileRepository;
        this.invitationRepository = invitationRepository;
        this.assignmentRepository = assignmentRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.mfaCredentialRepository = mfaCredentialRepository;
    }

    public AdminUserListItemResponse create(User user, boolean includeSensitiveIdentity) {
        var staffAssignments = assignmentRepository.findActive(
                user,
                AccessContext.STAFF,
                Instant.now()
        );
        List<RoleSummaryResponse> roles = staffAssignments.stream()
                .map(item -> item.getRole())
                .distinct()
                .map(role -> new RoleSummaryResponse(
                        role,
                        assignmentRepository.countActiveUsersByRole(role.getId(), Instant.now())
                ))
                .sorted(java.util.Comparator.comparing(RoleSummaryResponse::displayName))
                .toList();
        boolean citizen = !assignmentRepository.findActive(
                user,
                AccessContext.CITIZEN,
                Instant.now()
        ).isEmpty();
        boolean providerAccess = !assignmentRepository.findActive(
                user,
                AccessContext.PROVIDER,
                Instant.now()
        ).isEmpty();
        StaffProfile staffProfile = staffProfileRepository.findByUser(user).orElse(null);
        boolean staff = staffProfile != null
                && staffProfile.getEmploymentStatus() != StaffEmploymentStatus.REVOKED
                && staffProfile.getEmploymentStatus() != StaffEmploymentStatus.EXPIRED;

        AdminUserListItemResponse.ProviderAccountSummary provider = providerProfileRepository
                .findFirstByUserOrderByCreatedAtDesc(user)
                .map(this::providerSummary)
                .orElse(null);

        return AdminUserListItemResponse.basic(
                user,
                roles,
                incidentReportRepository.countByReporterUser(user),
                publicStoryRepository.countByAuthorUser(user),
                provider,
                new AdminUserListItemResponse.CapabilitySummary(citizen, providerAccess, staff),
                staffAccess(user, staffProfile, includeSensitiveIdentity),
                List.of()
        );
    }

    private AdminUserListItemResponse.StaffAccessSummary staffAccess(
            User user,
            StaffProfile profile,
            boolean includeSensitiveIdentity
    ) {
        if (profile != null) {
            return new AdminUserListItemResponse.StaffAccessSummary(
                    profile.getEmploymentStatus().name(),
                    null,
                    includeSensitiveIdentity ? user.getId() : null,
                    includeSensitiveIdentity ? user.getName() : null,
                    includeSensitiveIdentity ? user.getEmail() : null,
                    profile.getJobTitle(),
                    profile.getDepartment(),
                    profile.getExpiresAt(),
                    mfaCredentialRepository.findByUser(user)
                            .map(credential -> credential.isEnabled())
                            .orElse(false)
            );
        }
        var pending = invitationRepository
                .findFirstByLinkedProductUserAndStatusOrderByCreatedAtDesc(
                        user,
                        co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus.PENDING
                );
        if (pending.isPresent() && pending.get().getExpiresAt().isAfter(Instant.now())) {
            var invitation = pending.get();
            return new AdminUserListItemResponse.StaffAccessSummary(
                    "INVITATION_PENDING",
                    includeSensitiveIdentity ? invitation.getId() : null,
                    null,
                    null,
                    includeSensitiveIdentity ? user.getEmail() : null,
                    invitation.getJobTitle(),
                    invitation.getDepartment(),
                    invitation.getAccessExpiresAt(),
                    false
            );
        }
        return new AdminUserListItemResponse.StaffAccessSummary(
                "NOT_CREATED",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    private AdminUserListItemResponse.ProviderAccountSummary providerSummary(ProviderProfile profile) {
        return new AdminUserListItemResponse.ProviderAccountSummary(
                profile.getId(),
                profile.getProviderType() == null ? null : profile.getProviderType().name(),
                profile.getVerificationStatus() == null ? null : profile.getVerificationStatus().name(),
                profile.getAvailabilityStatus() == null ? null : profile.getAvailabilityStatus().name(),
                profile.getDisplayName()
        );
    }
}
