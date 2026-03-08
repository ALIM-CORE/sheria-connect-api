package co.tz.sheriaconnectapi.security.Jwt;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.UserRepository;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public JwtAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        System.out.println(path);
        return path.endsWith("/user")
                || path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.startsWith("/public")
                || path.equals("/login")
                || path.equals("/admin/login")
                || path.startsWith("/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!JwtUtil.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = JwtUtil.getClaims(token);
        String email = claims.getSubject();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found: " + email)
                );

        // Flatten: User → Roles → Authorities
        Set<SimpleGrantedAuthority> grantedAuthorities =
                user.getRoles().stream()
                        .flatMap(role -> role.getAuthorities().stream())
                        .map(authority ->
                                new SimpleGrantedAuthority(authority.getName())
                        )
                        .collect(Collectors.toSet());

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        grantedAuthorities
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
