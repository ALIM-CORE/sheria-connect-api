package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.RoleSummaryResponse;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ListRolesService implements Query<RoleAudience, List<RoleSummaryResponse>> {
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;

    public ListRolesService(
            RoleRepository roleRepository,
            UserRoleAssignmentRepository assignmentRepository
    ) {
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<List<RoleSummaryResponse>>> execute(RoleAudience audience) {
        var roles = audience == null
                ? roleRepository.findAllByOrderByDisplayNameAsc()
                : roleRepository.findByAudienceOrderByDisplayNameAsc(audience);
        List<RoleSummaryResponse> response = roles.stream()
                .map(role -> new RoleSummaryResponse(
                        role,
                        assignmentRepository.countActiveUsersByRole(role.getId(), Instant.now())
                ))
                .toList();
        return ResponseUtil.success(response, "Roles retrieved", HttpStatus.OK);
    }
}
