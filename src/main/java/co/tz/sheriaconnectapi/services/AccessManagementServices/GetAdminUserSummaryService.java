package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserSummaryResponse;
import co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.repositories.StaffInvitationRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import java.time.Instant;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetAdminUserSummaryService implements Query<Void, AdminUserSummaryResponse> {
    private final UserRepository userRepository;
    private final StaffInvitationRepository invitationRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final StaffProfileRepository staffProfileRepository;

    public GetAdminUserSummaryService(
            UserRepository userRepository,
            StaffInvitationRepository invitationRepository,
            UserRoleAssignmentRepository assignmentRepository,
            StaffProfileRepository staffProfileRepository
    ) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.assignmentRepository = assignmentRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<AdminUserSummaryResponse>> execute(Void input) {
        AdminUserSummaryResponse response = new AdminUserSummaryResponse(
                staffProfileRepository.count(),
                staffProfileRepository.countByEmploymentStatus(StaffEmploymentStatus.ACTIVE),
                assignmentRepository.countDistinctUsers(
                        AccessContext.CITIZEN,
                        RoleAssignmentStatus.ACTIVE,
                        Instant.now()
                ),
                assignmentRepository.countDistinctUsers(
                        AccessContext.CITIZEN,
                        RoleAssignmentStatus.ACTIVE,
                        Instant.now()
                ),
                assignmentRepository.countDistinctUsers(
                        AccessContext.PROVIDER,
                        RoleAssignmentStatus.ACTIVE,
                        Instant.now()
                ),
                assignmentRepository.countDistinctUsers(
                        AccessContext.PROVIDER,
                        RoleAssignmentStatus.ACTIVE,
                        Instant.now()
                ),
                userRepository.countByLockedTrue(),
                invitationRepository.countByStatus(StaffInvitationStatus.PENDING)
        );
        return ResponseUtil.success(response, "User summary retrieved", HttpStatus.OK);
    }
}
