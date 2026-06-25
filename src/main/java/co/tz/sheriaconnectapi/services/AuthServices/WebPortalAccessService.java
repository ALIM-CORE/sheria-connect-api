package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class WebPortalAccessService {
    private final ScopedAuthorityService scopedAuthorityService;

    public WebPortalAccessService(ScopedAuthorityService scopedAuthorityService) {
        this.scopedAuthorityService = scopedAuthorityService;
    }

    private static final Set<String> OWNER_ROLES = Set.of(
            "SUPER_ADMIN",
            "SYSTEM_ADMIN"
    );

    private static final Set<String> OWNER_AUTHORITIES = Set.of(
            "INCIDENTREPORT_READ",
            "PUBLICSTORY_READ",
            "PROVIDERPROFILE_READ",
            "CASEMATCHREQUEST_READ",
            "STORYCONTENTREPORT_READ",
            "USER_READ"
    );

    public boolean canAccessWebPortal(User user) {
        if (user == null
                || !Boolean.TRUE.equals(user.getActive())
                || Boolean.TRUE.equals(user.getLocked())) {
            return false;
        }

        return scopedAuthorityService.resolve(user, AccessContext.STAFF).roles().stream().anyMatch(role ->
                OWNER_ROLES.contains(role.getName())
                        || role.getAuthorities().stream().anyMatch(authority ->
                        OWNER_AUTHORITIES.contains(authority.getName())
                )
        );
    }
}
