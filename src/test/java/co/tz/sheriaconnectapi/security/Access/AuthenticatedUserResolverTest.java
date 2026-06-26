package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.exceptions.UnauthorizedCaseAccessException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserResolverTest {

    @Mock
    private UserRepository userRepository;

    private AuthenticatedUserResolver resolver;
    private User user;

    @BeforeEach
    void setUp() {
        resolver = new AuthenticatedUserResolver(userRepository);
        user = new User();
        user.setId(11L);
        user.setEmail("shared@sheriaconnect.co.tz");
    }

    @Test
    void resolvesTheSameIdentityInStaffContext() {
        var authentication = authentication(AccessContext.STAFF);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertEquals(user, resolver.requireUser(authentication));
        assertEquals(user, resolver.requireStaffUser(authentication));
        assertEquals(AccessContext.STAFF, resolver.requireContext(authentication));
    }

    @Test
    void citizenOnlyAccessRejectsAProviderSession() {
        var authentication = authentication(AccessContext.PROVIDER);

        assertThrows(
                UnauthorizedCaseAccessException.class,
                () -> resolver.requireCitizenUser(authentication)
        );
    }

    private UsernamePasswordAuthenticationToken authentication(AccessContext context) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of()
        );
        authentication.setDetails(new SessionAuthenticationDetails(
                "session-1",
                context,
                context.name().toLowerCase(),
                true
        ));
        return authentication;
    }
}
