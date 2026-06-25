package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.RoleAssignmentResponse;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffAssignmentQueryService {
    private final UserRepository userRepository;
    private final UserRoleAssignmentRepository assignmentRepository;

    public StaffAssignmentQueryService(
            UserRepository userRepository,
            UserRoleAssignmentRepository assignmentRepository
    ) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<List<RoleAssignmentResponse>>> list(Long userId) {
        var user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return ResponseUtil.success(
                assignmentRepository.findAllByUserAndContextOrderByCreatedAtDesc(
                                user,
                                AccessContext.STAFF
                        ).stream()
                        .map(RoleAssignmentResponse::new)
                        .toList(),
                "Staff assignment history retrieved",
                HttpStatus.OK
        );
    }
}
