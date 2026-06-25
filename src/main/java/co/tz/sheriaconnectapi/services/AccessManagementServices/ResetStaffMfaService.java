package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.MfaRecoveryCodeRepository;
import co.tz.sheriaconnectapi.repositories.StaffMfaCredentialRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.services.AuthServices.AuthSessionService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetStaffMfaService {
    private final UserRepository userRepository;
    private final StaffMfaCredentialRepository credentialRepository;
    private final MfaRecoveryCodeRepository recoveryCodeRepository;
    private final AuthSessionService sessionService;
    private final AccessManagementPolicy policy;
    private final AccessAuditService auditService;

    public ResetStaffMfaService(
            UserRepository userRepository,
            StaffMfaCredentialRepository credentialRepository,
            MfaRecoveryCodeRepository recoveryCodeRepository,
            AuthSessionService sessionService,
            AccessManagementPolicy policy,
            AccessAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.sessionService = sessionService;
        this.policy = policy;
        this.auditService = auditService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<Void>> execute(
            Long userId,
            String reason,
            Authentication authentication
    ) {
        if (reason == null || reason.isBlank()) {
            throw new AccessManagementException("A reason is required", HttpStatus.BAD_REQUEST);
        }
        var actor = policy.requireActor(authentication);
        var target = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        if (actor.getId().equals(target.getId())) {
            throw new AccessManagementException(
                    "You cannot reset your own MFA from access management",
                    HttpStatus.FORBIDDEN
            );
        }
        policy.requireCanManageStatus(actor, target);
        credentialRepository.findByUser(target).ifPresent(credential -> {
            recoveryCodeRepository.deleteAllByCredential(credential);
            credentialRepository.delete(credential);
        });
        sessionService.revokeContext(target.getId(), AccessContext.STAFF);
        auditService.log(actor, target, "STAFF_MFA_RESET", "USER", target.getId(), reason.trim());
        return ResponseUtil.success(null, "MFA reset. Enrollment is required at next login.", HttpStatus.OK);
    }
}
