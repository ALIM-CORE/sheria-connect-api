package co.tz.sheriaconnectapi.services.MatchingServices;

import co.tz.sheriaconnectapi.exceptions.UnauthorizedProviderProfileAccessException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProviderProfileAccessService {

    private final UserRepository userRepository;

    public ProviderProfileAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireAuthenticatedUser(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new UnauthorizedProviderProfileAccessException();
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(UnauthorizedProviderProfileAccessException::new);
    }
}
