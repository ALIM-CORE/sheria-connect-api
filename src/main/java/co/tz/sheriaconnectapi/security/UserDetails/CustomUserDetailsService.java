package co.tz.sheriaconnectapi.security.UserDetails;

import co.tz.sheriaconnectapi.exceptions.EmailNotVerifiedException;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException();
        }


        // Flatten: User → Roles → Authorities
        Set<SimpleGrantedAuthority> grantedAuthorities =
                user.getRoles().stream()
                        .flatMap(role -> role.getAuthorities().stream())
                        .map(authority ->
                                new SimpleGrantedAuthority(authority.getName())
                        )
                        .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(grantedAuthorities)
                .build();
    }
}
