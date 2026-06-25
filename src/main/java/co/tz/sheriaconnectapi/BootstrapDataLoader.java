package co.tz.sheriaconnectapi;

import co.tz.sheriaconnectapi.model.Entities.Authority;
import co.tz.sheriaconnectapi.model.Entities.Role;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.repositories.AuthorityRepository;
import co.tz.sheriaconnectapi.repositories.RoleRepository;
import co.tz.sheriaconnectapi.repositories.UserRepository;
import co.tz.sheriaconnectapi.repositories.UserRoleAssignmentRepository;
import co.tz.sheriaconnectapi.repositories.StaffProfileRepository;
import co.tz.sheriaconnectapi.model.Entities.UserRoleAssignment;
import co.tz.sheriaconnectapi.model.Entities.StaffProfile;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.RoleAssignmentStatus;
import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import jakarta.persistence.Entity;
import lombok.RequiredArgsConstructor;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
public class BootstrapDataLoader implements CommandLineRunner {
    private static final List<String> ACTIONS = List.of("CREATE", "READ", "UPDATE", "DELETE");
    private static final String ENTITY_PACKAGE = "co.tz.sheriaconnectapi.model.Entities";

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    private static final String CASE_MANAGER = "CASE_MANAGER";
    private static final String CASE_REVIEWER = "CASE_REVIEWER";
    private static final String CONTENT_MODERATOR = "CONTENT_MODERATOR";
    private static final String PROVIDER_REVIEWER = "PROVIDER_REVIEWER";
    private static final String SUPPORT_OFFICER = "SUPPORT_OFFICER";
    private static final String CITIZEN = "CITIZEN";
    private static final String PROVIDER = "PROVIDER";

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final StaffProfileRepository staffProfileRepository;

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
        bootstrapUser(superAdminEmail, superAdminPassword, superAdminName, roles.get(SUPER_ADMIN));
        bootstrapUser(systemAdminEmail, systemAdminPassword, systemAdminName, roles.get(SYSTEM_ADMIN));
        System.out.println("Bootstrapping completed successfully");
    }

    private Set<Authority> bootstrapAuthorities() {
        Set<Authority> authorities = new HashSet<>();
        for (Class<?> entityClass : new Reflections(ENTITY_PACKAGE).getTypesAnnotatedWith(Entity.class)) {
            String entityName = entityClass.getSimpleName().toUpperCase(Locale.ROOT);
            for (String action : ACTIONS) {
                String name = entityName + "_" + action;
                authorities.add(authorityRepository.findByName(name)
                        .orElseGet(() -> createAuthority(name)));
            }
        }
        return authorities;
    }

    private Map<String, Role> bootstrapRoles(Set<Authority> allAuthorities) {
        Map<String, Authority> authoritiesByName = allAuthorities.stream()
                .collect(Collectors.toMap(Authority::getName, authority -> authority));
        Map<String, Role> roles = new LinkedHashMap<>();

        for (RoleDefinition definition : roleDefinitions()) {
            Role role = roleRepository.findByName(definition.name())
                    .orElseGet(Role::new);
            role.setName(definition.name());
            role.setDisplayName(definition.displayName());
            role.setDescription(definition.description());
            role.setAudience(definition.audience());
            role.setSystemRole(true);
            role.setEditable(definition.editable());
            role.setDeletable(false);

            Set<Authority> resolved = definition.authorityNames().contains("*")
                    ? new HashSet<>(allAuthorities)
                    : definition.authorityNames().stream()
                    .map(authoritiesByName::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            role.setAuthorities(resolved);
            Role saved = roleRepository.save(role);
            roles.put(saved.getName(), saved);
        }
        return roles;
    }

    private List<RoleDefinition> roleDefinitions() {
        return List.of(
                staffRole(
                        SUPER_ADMIN,
                        "Super Admin",
                        "Full platform access for founding technical owners.",
                        false,
                        Set.of("*")
                ),
                staffRole(
                        SYSTEM_ADMIN,
                        "System Admin",
                        "Runs platform operations and manages operational staff.",
                        true,
                        systemAdminAuthorities()
                ),
                staffRole(
                        CASE_MANAGER,
                        "Case Manager",
                        "Reviews reports, updates cases, and coordinates provider matching.",
                        true,
                        caseManagerAuthorities()
                ),
                staffRole(
                        CASE_REVIEWER,
                        "Case Reviewer",
                        "Reviews case metadata and escalation queues without private communications.",
                        true,
                        caseReviewerAuthorities()
                ),
                staffRole(
                        CONTENT_MODERATOR,
                        "Content Moderator",
                        "Reviews public stories and reported content.",
                        true,
                        contentModeratorAuthorities()
                ),
                staffRole(
                        PROVIDER_REVIEWER,
                        "Provider Reviewer",
                        "Reviews and verifies advocate and NGO provider profiles.",
                        true,
                        providerReviewerAuthorities()
                ),
                staffRole(
                        SUPPORT_OFFICER,
                        "Support Officer",
                        "Finds accounts and manages Citizen or Provider account access.",
                        true,
                        Set.of("USER_READ", "USER_UPDATE")
                ),
                productRole(
                        CITIZEN,
                        "Citizen",
                        "Application-managed identity for Citizen app accounts.",
                        Set.of()
                ),
                productRole(
                        PROVIDER,
                        "Provider",
                        "Application-managed identity for Sheria Connect Pro accounts.",
                        providerAuthorities()
                )
        );
    }

    private RoleDefinition staffRole(
            String name,
            String displayName,
            String description,
            boolean editable,
            Set<String> authorities
    ) {
        return new RoleDefinition(
                name,
                displayName,
                description,
                RoleAudience.PLATFORM_STAFF,
                editable,
                authorities
        );
    }

    private RoleDefinition productRole(
            String name,
            String displayName,
            String description,
            Set<String> authorities
    ) {
        return new RoleDefinition(
                name,
                displayName,
                description,
                RoleAudience.PRODUCT_IDENTITY,
                false,
                authorities
        );
    }

    private Set<String> systemAdminAuthorities() {
        Set<String> authorities = new HashSet<>(Set.of(
                "USER_CREATE", "USER_READ", "USER_UPDATE", "ROLE_READ", "AUTHORITY_READ"
        ));
        authorities.addAll(caseManagerAuthorities());
        authorities.addAll(contentModeratorAuthorities());
        authorities.addAll(providerReviewerAuthorities());
        return authorities;
    }

    private Set<String> caseManagerAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.addAll(createReadUpdate("INCIDENTREPORT"));
        authorities.addAll(readOnly("EVIDENCEFILE"));
        authorities.addAll(createReadUpdate("CASESTATUSHISTORY"));
        authorities.addAll(createReadUpdate("ADMINCASENOTE"));
        authorities.addAll(createReadUpdate("CASEMATCHREQUEST"));
        authorities.addAll(readOnly("PROVIDERPROFILE"));
        return authorities;
    }

    private Set<String> caseReviewerAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.add("INCIDENTREPORT_READ");
        authorities.add("CASESTATUSHISTORY_READ");
        authorities.add("PROVIDERPROFILE_READ");
        return authorities;
    }

    private Set<String> contentModeratorAuthorities() {
        Set<String> authorities = new HashSet<>();
        authorities.addAll(createReadUpdate("PUBLICSTORY"));
        authorities.addAll(createReadUpdate("STORYCONTENTREPORT"));
        authorities.addAll(createReadUpdate("STORYMODERATIONNOTE"));
        return authorities;
    }

    private Set<String> providerReviewerAuthorities() {
        return new HashSet<>(Set.of("PROVIDERPROFILE_READ", "PROVIDERPROFILE_UPDATE"));
    }

    private Set<String> providerAuthorities() {
        return Set.of(
                "PROVIDERPROFILE_READ",
                "PROVIDERPROFILE_UPDATE",
                "CASEMATCHREQUEST_READ",
                "CASEMATCHREQUEST_UPDATE"
        );
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

    private void bootstrapUser(String email, String password, String name, Role role) {
        if (role == null || isBlank(email) || isBlank(password)) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(User::new);
        user.setName(isBlank(name) ? role.getDisplayName() : name.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password.trim()));
        user.setEmailVerified(true);
        user.setAccountType(UserAccountType.PLATFORM_STAFF);
        user.setActive(true);
        user.setLocked(false);
        user.getRoles().add(role);
        User saved = userRepository.save(user);

        boolean hasAssignment = !assignmentRepository.findActive(
                saved,
                AccessContext.STAFF,
                java.time.Instant.now()
        ).stream().filter(item -> item.getRole().getId().equals(role.getId())).toList().isEmpty();
        if (!hasAssignment) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(saved);
            assignment.setRole(role);
            assignment.setContext(AccessContext.STAFF);
            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
            assignment.setActivatedAt(java.time.Instant.now());
            assignment.setReason("Bootstrap owner account");
            assignmentRepository.save(assignment);
        }
        StaffProfile profile = staffProfileRepository.findByUser(saved).orElseGet(StaffProfile::new);
        profile.setUser(saved);
        profile.setEmploymentStatus(StaffEmploymentStatus.ACTIVE);
        profile.setJobTitle(role.getDisplayName());
        profile.setDepartment("Platform Operations");
        profile.setGrantReason("Bootstrap owner account");
        if (profile.getActivatedAt() == null) {
            profile.setActivatedAt(java.time.Instant.now());
        }
        staffProfileRepository.save(profile);
    }

    private Authority createAuthority(String name) {
        Authority authority = new Authority();
        authority.setName(name);
        return authorityRepository.save(authority);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record RoleDefinition(
            String name,
            String displayName,
            String description,
            RoleAudience audience,
            boolean editable,
            Set<String> authorityNames
    ) {
    }
}
