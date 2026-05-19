package co.tz.sheriaconnectapi.services.AuthServices;

import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class WebPortalAccessService {

    private static final Set<String> OWNER_ROLES = Set.of(
            "SUPER_ADMIN",
            "SYSTEM_ADMIN"
    );

    private static final Set<String> OWNER_AUTHORITIES = Set.of(
            "INCIDENTREPORT_READ",
            "PUBLICSTORY_READ",
            "PROVIDERPROFILE_READ",
            "CASEMATCHREQUEST_READ",
            "STORYCONTENTREPORT_READ"
    );

    public boolean canAccessWebPortal(User user) {
        if (user == null) {
            return false;
        }

        return user.getRoles().stream().anyMatch(role ->
                OWNER_ROLES.contains(role.getName())
                        || role.getAuthorities().stream().anyMatch(authority ->
                        OWNER_AUTHORITIES.contains(authority.getName())
                )
        );
    }
}
