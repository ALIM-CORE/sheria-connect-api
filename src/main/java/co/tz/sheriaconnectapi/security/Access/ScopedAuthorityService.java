package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.exceptions.WebPortalAccessDeniedException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScopedAuthorityService {
    private final UserRoleAssignmentRepository assignmentRepository;

    public ScopedAuthorityService(UserRoleAssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public EffectiveAccess resolve(User user, AccessContext context) {
        List<UserRoleAssignment> assignments = assignmentRepository.findActive(
                user,
                context,
                Instant.now()
        );
        List<co.tz.sheriaconnectapi.model.Entities.Role> roles = assignments.stream()
                .map(UserRoleAssignment::getRole)
                .distinct()
                .toList();
        Set<SimpleGrantedAuthority> authorities = roles.stream()
                .flatMap(role -> role.getAuthorities().stream())
                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                .collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("CONTEXT_" + context.name()));
        return new EffectiveAccess(context, roles, authorities);
    }

    public EffectiveAccess require(User user, AccessContext context) {
        EffectiveAccess access = resolve(user, context);
        if (access.roles().isEmpty()) {
            throw new WebPortalAccessDeniedException();
        }
        return access;
    }
}
