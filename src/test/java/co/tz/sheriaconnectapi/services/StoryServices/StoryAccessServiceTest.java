package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.security.Access.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryAccessServiceTest {

    @Mock
    private AuthenticatedUserResolver authenticatedUserResolver;
    @Mock
    private Authentication authentication;

    @Test
    void optionalViewerTreatsProviderAndStaffAsAnonymous() {
        User user = new User();
        user.setId(5L);
        StoryAccessService service = new StoryAccessService(authenticatedUserResolver);

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(authenticatedUserResolver.requireContext(authentication))
                .thenReturn(AccessContext.PROVIDER);

        assertTrue(service.authenticatedUser(authentication).isEmpty());
        verify(authenticatedUserResolver, never()).requireStoryAuthor(authentication);
    }

    @Test
    void optionalViewerReturnsCitizenUser() {
        User user = new User();
        user.setId(6L);
        StoryAccessService service = new StoryAccessService(authenticatedUserResolver);

        when(authenticatedUserResolver.authenticatedUser(authentication))
                .thenReturn(Optional.of(user));
        when(authenticatedUserResolver.requireContext(authentication))
                .thenReturn(AccessContext.CITIZEN);

        assertEquals(user, service.authenticatedUser(authentication).orElseThrow());
    }

    @Test
    void requiredStoryUserRemainsStrictCitizenAuthor() {
        User user = new User();
        user.setId(7L);
        StoryAccessService service = new StoryAccessService(authenticatedUserResolver);

        when(authenticatedUserResolver.requireStoryAuthor(authentication))
                .thenReturn(user);

        assertEquals(user, service.requireAuthenticatedUser(authentication));
    }
}
