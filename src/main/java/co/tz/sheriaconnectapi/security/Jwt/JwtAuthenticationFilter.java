package co.tz.sheriaconnectapi.security.Jwt;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.AuthSessionRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.security.Access.ScopedAuthorityService;
import co.tz.sheriaconnectapi.security.Access.SessionAuthenticationDetails;
import co.tz.sheriaconnectapi.security.Handlers.ApiAccessDeniedHandler;
import co.tz.sheriaconnectapi.security.Handlers.ApiAuthenticationEntryPoint;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;
    private final AuthSessionRepository sessionRepository;
    private final ScopedAuthorityService authorityService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    public JwtAuthenticationFilter(
            UserRepository userRepository,
            AuthSessionRepository sessionRepository,
            ScopedAuthorityService authorityService,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.authorityService = authorityService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/");
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
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid or expired access token")
            );
            return;
        }

        Claims claims = JwtUtil.getClaims(token);
        Long userId;
        AccessContext context;
        String sessionId;
        Set<String> audiences;
        Boolean mfaVerified;
        try {
            userId = Long.valueOf(claims.getSubject());
            context = AccessContext.valueOf(claims.get("active_context", String.class));
            sessionId = claims.get("sid", String.class);
            audiences = claims.getAudience();
            mfaVerified = claims.get("mfa_verified", Boolean.class);
        } catch (RuntimeException exception) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid access token", exception)
            );
            return;
        }

        AuthSession session = sessionRepository.findBySessionId(sessionId).orElse(null);
        User user = userRepository.findDetailedById(userId).orElse(null);
        if (session == null
                || user == null
                || session.isRevoked()
                || session.getExpiresAt().isBefore(Instant.now())
                || !session.getUser().getId().equals(userId)
                || session.getActiveContext() != context
                || audiences == null
                || !audiences.contains(session.getAudience())
                || !Boolean.TRUE.equals(user.getActive())
                || Boolean.TRUE.equals(user.getLocked())
                || (context == AccessContext.STAFF
                    && (!session.isMfaVerified() || !Boolean.TRUE.equals(mfaVerified)))) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Authentication session is invalid or expired")
            );
            return;
        }

        if (!contextAllowedForPath(context, request.getRequestURI())) {
            accessDeniedHandler.handle(
                    request,
                    response,
                    new AccessDeniedException("Active context cannot access this resource")
            );
            return;
        }

        var effectiveAccess = authorityService.resolve(user, context);
        if (effectiveAccess.roles().isEmpty()) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("No active access assignment")
            );
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        effectiveAccess.authorities()
                );
        authentication.setDetails(new SessionAuthenticationDetails(
                sessionId,
                context,
                session.getAudience(),
                session.isMfaVerified()
        ));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setLastActivityAt(Instant.now());
        sessionRepository.save(session);
        filterChain.doFilter(request, response);
    }

    private boolean contextAllowedForPath(AccessContext context, String path) {
        if (path.startsWith("/admin/")) {
            return context == AccessContext.STAFF;
        }
        if (path.startsWith("/provider-profile")
                || path.startsWith("/provider/")) {
            return context == AccessContext.PROVIDER;
        }
        if (path.equals("/incident-reports/mine")
                || (path.startsWith("/incident-reports/") && path.endsWith("/messages"))
                || path.equals("/stories/mine")
                || path.equals("/stories/bookmarks")) {
            return context == AccessContext.CITIZEN;
        }
        return true;
    }
}
