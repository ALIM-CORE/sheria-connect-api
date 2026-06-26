package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.exceptions.UnauthorizedProviderProfileAccessException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProviderProfileAccessService {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ProviderProfileAccessService(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    public User requireAuthenticatedUser(Authentication authentication) {
        return authenticatedUserResolver.requireProviderUser(authentication);
    }
}
