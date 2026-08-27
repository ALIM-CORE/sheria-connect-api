package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StoryAccessService {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public StoryAccessService(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    public Optional<User> authenticatedUser(Authentication authentication) {
        Optional<User> user = authenticatedUserResolver.authenticatedUser(authentication);
        if (user.isEmpty()) {
            return Optional.empty();
        }
        if (authenticatedUserResolver.requireContext(authentication) == AccessContext.CITIZEN) {
            return user;
        }
        return Optional.empty();
    }

    public User requireAuthenticatedUser(Authentication authentication) {
        return authenticatedUserResolver.requireStoryAuthor(authentication);
    }

    public boolean isOwner(PublicStory story, User user) {
        return story.getAuthorUser() != null
                && user != null
                && story.getAuthorUser().getId().equals(user.getId());
    }
}
