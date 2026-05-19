package co.tz.sheriaconnectapi;

import co.tz.sheriaconnectapi.model.Entities.Authority;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.AuthorityRepository;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataLoader implements CommandLineRunner {

    private static final List<String> ACTIONS = List.of(
            "CREATE",
            "READ",
            "UPDATE",
            "DELETE"
    );

    private static final String ENTITY_PACKAGE = "co.tz.sheriaconnectapi.model.Entities";
    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.super-admin.email:}")
    private String superAdminEmail;

    @Value("${app.bootstrap.super-admin.password:}")
    private String superAdminPassword;

    @Value("${app.bootstrap.super-admin.name:Super Admin}")
    private String superAdminName;

    @Value("${app.bootstrap.system-admin.email:}")
    private String systemAdminEmail;

    @Value("${app.bootstrap.system-admin.password:}")
    private String systemAdminPassword;

    @Value("${app.bootstrap.system-admin.name:System Admin}")
    private String systemAdminName;

    @Override
    public void run(String... args) {
        System.out.println("Bootstrapping authorities, roles, and owner users...");

        Set<Authority> allAuthorities = bootstrapAuthorities();
        Map<String, Role> roles = bootstrapRoles(allAuthorities);
        bootstrapUser(
                SUPER_ADMIN,
                superAdminEmail,
                superAdminPassword,
                superAdminName,
                roles.get(SUPER_ADMIN)
        );
        bootstrapUser(
                SYSTEM_ADMIN,
                systemAdminEmail,
                systemAdminPassword,
                systemAdminName,
                roles.get(SYSTEM_ADMIN)
        );

        System.out.println("Bootstrapping completed successfully");
    }

    private Set<Authority> bootstrapAuthorities() {
        Reflections reflections = new Reflections(ENTITY_PACKAGE);
        Set<Class<?>> entities = reflections.getTypesAnnotatedWith(Entity.class);

        Set<Authority> allAuthorities = new HashSet<>();

        for (Class<?> entityClass : entities) {
            String entityName = entityClass.getSimpleName().toUpperCase(Locale.ROOT);

            for (String action : ACTIONS) {
                String authorityName = entityName + "_" + action;
                Authority authority = authorityRepository.findByName(authorityName)
                        .orElseGet(() -> createAuthority(authorityName));

                allAuthorities.add(authority);
            }
        }

        System.out.println("Total authorities available: " + allAuthorities.size());
        return allAuthorities;
    }

    private Map<String, Role> bootstrapRoles(Set<Authority> allAuthorities) {
        Map<String, Set<String>> roleAuthorityMap = buildRoleAuthorityMap();
        Map<String, Role> roles = new LinkedHashMap<>();

        for (Map.Entry<String, Set<String>> entry : roleAuthorityMap.entrySet()) {
            String roleName = entry.getKey();
            Set<String> authorityNames = entry.getValue();

            Role role = roleRepository.findByName(roleName)
                    .orElseGet(() -> createRole(roleName));

            Set<Authority> resolvedAuthorities = authorityNames.contains("*")
                    ? new HashSet<>(allAuthorities)
                    : resolveAuthorities(authorityNames, allAuthorities);

            role.setAuthorities(resolvedAuthorities);
            Role savedRole = roleRepository.save(role);
            roles.put(roleName, savedRole);

            System.out.println(
                    "Assigned " + resolvedAuthorities.size() + " authorities to role: " + roleName
            );
        }

        return roles;
    }

    private Set<Authority> resolveAuthorities(
            Set<String> authorityNames,
            Set<Authority> allAuthorities
    ) {
        Map<String, Authority> availableAuthorities = allAuthorities.stream()
                .collect(Collectors.toMap(Authority::getName, authority -> authority));

        Set<Authority> resolvedAuthorities = new HashSet<>();
        for (String authorityName : authorityNames) {
            Authority authority = availableAuthorities.get(authorityName);
            if (authority == null) {
                System.out.println("Skipping missing authority: " + authorityName);
                continue;
            }
            resolvedAuthorities.add(authority);
        }

        return resolvedAuthorities;
    }

    private void bootstrapUser(
            String roleName,
            String email,
            String password,
            String name,
            Role role
    ) {
        if (role == null) {
            throw new IllegalStateException("Role not found during bootstrap: " + roleName);
        }

        if (isBlank(email) || isBlank(password)) {
            System.out.println(
                    "Skipping " + roleName + " bootstrap user because email/password is not configured"
            );
            return;
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(User::new);

        user.setName(isBlank(name) ? roleName : name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password.trim()));
        user.setEmailVerified(true);
        user.getRoles().add(role);

        userRepository.save(user);
        System.out.println(roleName + " bootstrap account is ready for: " + normalizedEmail);
    }

    private Authority createAuthority(String authorityName) {
        Authority authority = new Authority();
        authority.setName(authorityName);
        Authority savedAuthority = authorityRepository.save(authority);
        System.out.println("Created authority: " + authorityName);
        return savedAuthority;
    }

    private Role createRole(String roleName) {
        Role role = new Role();
        role.setName(roleName);
        Role savedRole = roleRepository.save(role);
        System.out.println("Created role: " + roleName);
        return savedRole;
    }

    private Map<String, Set<String>> buildRoleAuthorityMap() {
        Map<String, Set<String>> roleAuthorityMap = new LinkedHashMap<>();
        roleAuthorityMap.put(SUPER_ADMIN, Set.of("*"));
        roleAuthorityMap.put(SYSTEM_ADMIN, systemAdminAuthorities());
        return roleAuthorityMap;
    }

    private Set<String> systemAdminAuthorities() {
        Set<String> authorities = new HashSet<>();

        authorities.add("USER_READ");
        authorities.add("USER_UPDATE");
        authorities.add("ROLE_READ");
        authorities.add("AUTHORITY_READ");

        authorities.addAll(createReadUpdate("INCIDENTREPORT"));
        authorities.addAll(readOnly("EVIDENCEFILE"));
        authorities.addAll(createReadUpdate("CASESTATUSHISTORY"));
        authorities.addAll(createReadUpdate("ADMINCASENOTE"));
        authorities.addAll(createReadUpdate("PROVIDERPROFILE"));
        authorities.addAll(createReadUpdate("CASEMATCHREQUEST"));

        authorities.add("PASSWORDRESETTOKEN_READ");
        authorities.add("EMAILVERIFICATIONTOKEN_READ");

        return authorities;
    }

    private Set<String> createReadUpdate(String entityName) {
        return Set.of(
                entityName + "_CREATE",
                entityName + "_READ",
                entityName + "_UPDATE"
        );
    }

    private Set<String> readOnly(String entityName) {
        return Set.of(entityName + "_READ");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
