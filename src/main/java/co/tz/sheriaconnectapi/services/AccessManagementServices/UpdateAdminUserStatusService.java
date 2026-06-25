package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.model.DTOs.UpdateUserStatusRequest;
import co.tz.sheriaconnectapi.repositories.RefreshTokenRepository;
import co.tz.sheriaconnectapi.services.AuthServices.AuthSessionService;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UpdateAdminUserStatusService {
    private final UserRepository userRepository;
    private final AuthSessionService authSessionService;
    private final AccessManagementPolicy policy;
    private final AdminUserResponseFactory responseFactory;
    private final AccessAuditService auditService;

    public UpdateAdminUserStatusService(
            UserRepository userRepository,
            AuthSessionService authSessionService,
            AccessManagementPolicy policy,
            AdminUserResponseFactory responseFactory,
            AccessAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.authSessionService = authSessionService;
        this.policy = policy;
        this.responseFactory = responseFactory;
        this.auditService = auditService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> execute(
            Long userId,
            UpdateUserStatusRequest request,
            Authentication authentication
    ) {
        if (request == null || (request.active() == null && request.locked() == null)) {
            throw new AccessManagementException(
                    "Provide an active or locked state",
                    HttpStatus.BAD_REQUEST
            );
        }

        var actor = policy.requireActor(authentication);
        var target = userRepository.findDetailedById(userId)
                .orElseThrow(UserNotFoundException::new);
        policy.requireCanManageStatus(actor, target);

        boolean nextActive = request.active() == null
                ? Boolean.TRUE.equals(target.getActive())
                : request.active();
        boolean nextLocked = request.locked() == null
                ? Boolean.TRUE.equals(target.getLocked())
                : request.locked();

        policy.ensureLastSuperAdminRemainsUsable(
                target,
                target.getRoles(),
                nextActive,
                nextLocked
        );

        target.setActive(nextActive);
        target.setLocked(nextLocked);
        if (!nextActive || nextLocked) {
            String reason = request.reason() == null ? null : request.reason().trim();
            if (reason == null || reason.isBlank()) {
                throw new AccessManagementException(
                        "A reason is required when suspending or locking an account",
                        HttpStatus.BAD_REQUEST
                );
            }
            target.setSuspensionReason(reason);
            target.setSuspendedAt(Instant.now());
        } else {
            target.setSuspensionReason(null);
            target.setSuspendedAt(null);
        }

        var saved = userRepository.save(target);
        authSessionService.revokeAll(saved.getId());
        auditService.log(
                actor,
                saved,
                "USER_STATUS_UPDATED",
                "USER",
                saved.getId(),
                "Active: " + nextActive + ", locked: " + nextLocked
        );
        return ResponseUtil.success(
                responseFactory.create(saved, policy.canViewLinkedIdentity(authentication)),
                "Account status updated",
                HttpStatus.OK
        );
    }
}
