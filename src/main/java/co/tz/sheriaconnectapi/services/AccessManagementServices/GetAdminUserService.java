package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.UserNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.AdminUserListItemResponse;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Service
public class GetAdminUserService {
    private final UserRepository userRepository;
    private final AdminUserResponseFactory responseFactory;
    private final AccessManagementPolicy policy;

    public GetAdminUserService(
            UserRepository userRepository,
            AdminUserResponseFactory responseFactory,
            AccessManagementPolicy policy
    ) {
        this.userRepository = userRepository;
        this.responseFactory = responseFactory;
        this.policy = policy;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<AdminUserListItemResponse>> execute(
            Long id,
            Authentication authentication
    ) {
        var user = userRepository.findDetailedById(id)
                .orElseThrow(UserNotFoundException::new);
        return ResponseUtil.success(
                responseFactory.create(user, policy.canViewLinkedIdentity(authentication)),
                "User retrieved",
                HttpStatus.OK
        );
    }
}
