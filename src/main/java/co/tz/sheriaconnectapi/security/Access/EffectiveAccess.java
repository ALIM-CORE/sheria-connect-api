package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

public record EffectiveAccess(
        AccessContext context,
        List<Role> roles,
        Set<SimpleGrantedAuthority> authorities
) {
}
