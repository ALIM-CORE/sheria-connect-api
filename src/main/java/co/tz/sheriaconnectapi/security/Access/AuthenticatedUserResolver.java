package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.exceptions.AuthenticationRequiredException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedProviderProfileAccessException;
import co.tz.sheriaconnectapi.exceptions.UnauthorizedStoryAccessException;
import co.tz.sheriaconnectapi.exceptions.WebPortalAccessDeniedException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticatedUser(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    public User requireUser(Authentication authentication) {
        return authenticatedUser(authentication)
                .orElseThrow(AuthenticationRequiredException::new);
    }

    public AccessContext requireContext(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException();
        }
        if (authentication.getDetails() instanceof SessionAuthenticationDetails details) {
            return details.context();
        }
        throw new AuthenticationRequiredException();
    }

    public User requireCitizenUser(Authentication authentication) {
        if (requireContext(authentication) != AccessContext.CITIZEN) {
            throw new UnauthorizedCaseAccessException();
        }
        return requireUser(authentication);
    }

    public User requireProviderUser(Authentication authentication) {
        if (requireContext(authentication) != AccessContext.PROVIDER) {
            throw new UnauthorizedProviderProfileAccessException();
        }
        return requireUser(authentication);
    }

    public User requireStaffUser(Authentication authentication) {
        if (requireContext(authentication) != AccessContext.STAFF) {
            throw new WebPortalAccessDeniedException();
        }
        return requireUser(authentication);
    }

    public User requireStoryAuthor(Authentication authentication) {
        if (requireContext(authentication) != AccessContext.CITIZEN) {
            throw new UnauthorizedStoryAccessException();
        }
        return requireUser(authentication);
    }
}
