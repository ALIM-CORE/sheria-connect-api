package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.services.AuthServices.AuthSessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class StaffAccessExpiryService {
    private static final List<StaffEmploymentStatus> EXPIRABLE_STATUSES = List.of(
            StaffEmploymentStatus.ACTIVE,
            StaffEmploymentStatus.SUSPENDED
    );

    private final StaffProfileRepository staffProfileRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final AuthSessionService sessionService;

    public StaffAccessExpiryService(
            StaffProfileRepository staffProfileRepository,
            UserRoleAssignmentRepository assignmentRepository,
            AuthSessionService sessionService
    ) {
        this.staffProfileRepository = staffProfileRepository;
        this.assignmentRepository = assignmentRepository;
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelayString = "${app.auth.access-expiry-check-ms:60000}")
    @Transactional
    public void expireAccess() {
        Instant now = Instant.now();
        List<Long> userIds = staffProfileRepository.findExpiredUserIds(
                EXPIRABLE_STATUSES,
                now
        );
        if (userIds.isEmpty()) {
            return;
        }

        assignmentRepository.expireAssignments(now);
        staffProfileRepository.expireProfiles(EXPIRABLE_STATUSES, now);
        userIds.forEach(userId -> sessionService.revokeContext(userId, AccessContext.STAFF));
    }
}
