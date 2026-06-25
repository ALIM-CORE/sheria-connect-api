package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.exceptions.UnauthorizedStoryAccessException;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import co.tz.sheriaconnectapi.security.Access.SessionAuthenticationDetails;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;

@Service
public class StoryAccessService {

    private final UserRepository userRepository;

    public StoryAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return Optional.empty();
        }
        if (authentication.getDetails() instanceof SessionAuthenticationDetails details
                && details.context() != AccessContext.CITIZEN) {
            throw new UnauthorizedStoryAccessException();
        }

        return userRepository.findByEmail(authentication.getName());
    }

    public User requireAuthenticatedUser(Authentication authentication) {
        return authenticatedUser(authentication)
                .orElseThrow(UnauthorizedStoryAccessException::new);
    }

    public boolean isOwner(PublicStory story, User user) {
        return story.getAuthorUser() != null
                && user != null
                && story.getAuthorUser().getId().equals(user.getId());
    }
}
