package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.exceptions.AccessManagementException;
import co.tz.sheriaconnectapi.model.Commands.LoginResponse;
import co.tz.sheriaconnectapi.model.DTOs.*;
import co.tz.sheriaconnectapi.model.Entities.*;
import co.tz.sheriaconnectapi.model.Enums.*;
import co.tz.sheriaconnectapi.repositories.*;
import co.tz.sheriaconnectapi.services.AuthServices.AuthChallengeService;
import co.tz.sheriaconnectapi.services.EmailService;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class StaffInvitationService {
    private final StaffInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleAssignmentRepository assignmentRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffMfaCredentialRepository mfaCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final InvitationTokenService tokenService;
    private final AuthChallengeService authChallengeService;
    private final AccessManagementPolicy policy;
    private final AccessAuditService auditService;
    private final EmailService emailService;

    @Value("${app.frontend.base-domain}")
    private String frontendBaseDomain;

    public StaffInvitationService(
            StaffInvitationRepository invitationRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleAssignmentRepository assignmentRepository,
            StaffProfileRepository staffProfileRepository,
            StaffMfaCredentialRepository mfaCredentialRepository,
            PasswordEncoder passwordEncoder,
            InvitationTokenService tokenService,
            AuthChallengeService authChallengeService,
            AccessManagementPolicy policy,
            AccessAuditService auditService,
            EmailService emailService
    ) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.assignmentRepository = assignmentRepository;
        this.staffProfileRepository = staffProfileRepository;
        this.mfaCredentialRepository = mfaCredentialRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authChallengeService = authChallengeService;
        this.policy = policy;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    @Transactional
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> create(
            CreateStaffInvitationRequest request,
            Authentication authentication
    ) {
        User actor = policy.requireActor(authentication);
        String name = normalizeRequired(request == null ? null : request.name(), "Name");
        String email = normalizeEmail(request == null ? null : request.email());
        Set<Role> roles = resolveRoles(request == null ? null : request.roleIds());
        String jobTitle = normalizeRequired(request == null ? null : request.jobTitle(), "Job title");
        String department = normalizeRequired(request == null ? null : request.department(), "Department");
        String reason = normalizeRequired(request == null ? null : request.reason(), "Reason");
        policy.requireCanInvite(actor, roles);

        User existingUser = userRepository.findByEmail(email).orElse(null);
        StaffInvitation saved;
        if (existingUser != null) {
            saved = createExistingUserInvitation(
                    actor,
                    existingUser,
                    roles,
                    jobTitle,
                    department,
                    request.employeeNumber(),
                    request.workEmail(),
                    request.expiresAt(),
                    reason
            );
        } else {
            ensureNoPendingEmailInvitation(email);
            saved = createInvitation(
                    actor,
                    name,
                    email,
                    roles,
                    null,
                    jobTitle,
                    department,
                    request.employeeNumber(),
                    request.workEmail(),
                    request.expiresAt(),
                    reason
            );
        }

        auditService.log(
                actor,
                existingUser,
                "STAFF_INVITATION_CREATED",
                "STAFF_INVITATION",
                saved.getId(),
                saved.getEmail()
        );
        return ResponseUtil.success(
                new StaffInvitationResponse(saved),
                "Staff access invitation sent",
                HttpStatus.CREATED
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> createLinked(
            Long userId,
            CreateLinkedStaffInvitationRequest request,
            Authentication authentication
    ) {
        User actor = policy.requireActor(authentication);
        User target = userRepository.findDetailedById(userId)
                .orElseThrow(() -> new AccessManagementException(
                        "Account not found",
                        HttpStatus.NOT_FOUND
                ));
        Set<Role> roles = resolveRoles(request == null ? null : request.roleIds());
        String jobTitle = normalizeRequired(request == null ? null : request.jobTitle(), "Job title");
        String department = normalizeRequired(request == null ? null : request.department(), "Department");
        String reason = normalizeRequired(request == null ? null : request.reason(), "Reason");
        policy.requireCanInvite(actor, roles);

        StaffInvitation saved = createExistingUserInvitation(
                actor,
                target,
                roles,
                jobTitle,
                department,
                request.employeeNumber(),
                request.workEmail(),
                request.expiresAt(),
                reason
        );
        auditService.log(
                actor,
                target,
                "STAFF_INVITATION_CREATED",
                "STAFF_INVITATION",
                saved.getId(),
                reason
        );
        return ResponseUtil.success(
                new StaffInvitationResponse(saved),
                "Staff access invitation sent to the existing account",
                HttpStatus.CREATED
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<StandardResponse<List<StaffInvitationResponse>>> list() {
        return ResponseUtil.success(
                invitationRepository.findAllByOrderByCreatedAtDesc()
                        .stream()
                        .map(StaffInvitationResponse::new)
                        .toList(),
                "Staff invitations retrieved",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> resend(
            Long invitationId,
            Authentication authentication
    ) {
        User actor = policy.requireActor(authentication);
        StaffInvitation invitation = requireInvitation(invitationId);
        policy.requireCanInvite(actor, invitation.getRoles());
        if (invitation.getStatus() == StaffInvitationStatus.ACCEPTED
                || invitation.getStatus() == StaffInvitationStatus.REVOKED) {
            throw badRequest("Only pending or expired invitations can be resent");
        }

        String rawToken = tokenService.generate();
        invitation.setTokenHash(tokenService.hash(rawToken));
        invitation.setStatus(StaffInvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        invitation.setRevokedAt(null);
        StaffInvitation saved = invitationRepository.save(invitation);
        sendInvitation(saved, rawToken);
        auditService.log(actor, invitation.getLinkedProductUser(), "STAFF_INVITATION_RESENT",
                "STAFF_INVITATION", saved.getId(), saved.getEmail());
        return ResponseUtil.success(
                new StaffInvitationResponse(saved),
                "Staff invitation resent",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<StaffInvitationResponse>> revoke(
            Long invitationId,
            Authentication authentication
    ) {
        User actor = policy.requireActor(authentication);
        StaffInvitation invitation = requireInvitation(invitationId);
        policy.requireCanInvite(actor, invitation.getRoles());
        if (invitation.getStatus() != StaffInvitationStatus.PENDING) {
            throw badRequest("Only pending invitations can be revoked");
        }
        invitation.setStatus(StaffInvitationStatus.REVOKED);
        invitation.setRevokedAt(Instant.now());
        assignmentRepository.findAllByStaffInvitation(invitation).forEach(assignment -> {
            assignment.setStatus(RoleAssignmentStatus.REVOKED);
            assignment.setChangedByUser(actor);
            assignment.setRevokedAt(Instant.now());
            assignmentRepository.save(assignment);
        });
        if (invitation.getLinkedProductUser() != null) {
            staffProfileRepository.findByUser(invitation.getLinkedProductUser()).ifPresent(profile -> {
                if (profile.getEmploymentStatus() == StaffEmploymentStatus.PENDING) {
                    staffProfileRepository.delete(profile);
                }
            });
        }
        StaffInvitation saved = invitationRepository.save(invitation);
        auditService.log(actor, invitation.getLinkedProductUser(), "STAFF_INVITATION_REVOKED",
                "STAFF_INVITATION", saved.getId(), saved.getGrantReason());
        return ResponseUtil.success(
                new StaffInvitationResponse(saved),
                "Staff invitation revoked",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<StaffInvitationValidationResponse>> validate(
            String rawToken
    ) {
        StaffInvitation invitation = requireUsableToken(rawToken);
        return ResponseUtil.success(
                new StaffInvitationValidationResponse(
                        invitation.getName(),
                        invitation.getEmail(),
                        invitation.getExpiresAt(),
                        invitation.getRoles().stream()
                                .map(Role::getDisplayName)
                                .sorted()
                                .toList(),
                        invitation.getLinkedProductUser() != null
                                || userRepository.existsByEmail(invitation.getEmail())
                ),
                "Invitation is valid",
                HttpStatus.OK
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<UserDTO>> accept(
            AcceptStaffInvitationRequest request
    ) {
        if (request == null || request.password() == null || request.password().length() < 8) {
            throw badRequest("Password must contain at least 8 characters");
        }
        StaffInvitation invitation = requireUsableToken(request.token());
        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw conflict("Sign in with the existing account to accept this invitation");
        }

        User user = new User();
        user.setName(invitation.getName());
        user.setEmail(invitation.getEmail());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmailVerified(true);
        user.setAccountType(UserAccountType.PLATFORM_STAFF);
        user.setActive(true);
        user.setLocked(false);
        User savedUser = userRepository.save(user);

        activateStaffAccess(savedUser, invitation);
        markAccepted(invitation, savedUser);
        auditService.log(savedUser, savedUser, "STAFF_INVITATION_ACCEPTED",
                "STAFF_INVITATION", invitation.getId(), savedUser.getEmail());
        return ResponseUtil.success(
                new UserDTO(savedUser),
                "Account created. Sign in to configure multi-factor authentication.",
                HttpStatus.CREATED
        );
    }

    @Transactional
    public ResponseEntity<StandardResponse<LoginResponse>> acceptExisting(
            AcceptExistingStaffInvitationRequest request
    ) {
        AuthChallenge challenge = authChallengeService.require(
                request == null ? null : request.challengeToken(),
                AuthChallengePurpose.INVITATION_ACCEPTANCE
        );
        StaffInvitation invitation = challenge.getStaffInvitation();
        if (invitation == null || invitation.getStatus() != StaffInvitationStatus.PENDING) {
            throw badRequest("Invitation is no longer available");
        }
        User user = challenge.getUser();
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw badRequest("This invitation belongs to another account");
        }

        activateStaffAccess(user, invitation);
        markAccepted(invitation, user);
        authChallengeService.consume(challenge);

        boolean mfaEnabled = mfaCredentialRepository.findByUser(user)
                .map(StaffMfaCredential::isEnabled)
                .orElse(false);
        String nextChallenge = authChallengeService.create(
                user,
                mfaEnabled ? AuthChallengePurpose.MFA_VERIFY : AuthChallengePurpose.MFA_SETUP,
                null
        );
        auditService.log(user, user, "STAFF_INVITATION_ACCEPTED",
                "STAFF_INVITATION", invitation.getId(), invitation.getGrantReason());
        return ResponseUtil.success(
                new LoginResponse(
                        null,
                        new UserDTO(user),
                        mfaEnabled ? AuthState.MFA_REQUIRED : AuthState.MFA_SETUP_REQUIRED,
                        nextChallenge
                ),
                mfaEnabled
                        ? "Staff access activated. Verify MFA to continue."
                        : "Staff access activated. Configure MFA to continue.",
                HttpStatus.OK
        );
    }

    private StaffInvitation createExistingUserInvitation(
            User actor,
            User user,
            Set<Role> roles,
            String jobTitle,
            String department,
            String employeeNumber,
            String workEmail,
            Instant accessExpiresAt,
            String reason
    ) {
        StaffProfile existingProfile = staffProfileRepository.findByUser(user).orElse(null);
        if (existingProfile != null
                && existingProfile.getEmploymentStatus() != StaffEmploymentStatus.REVOKED
                && existingProfile.getEmploymentStatus() != StaffEmploymentStatus.EXPIRED) {
            throw conflict("This account already has staff access");
        }
        invitationRepository.findFirstByLinkedProductUserAndStatusOrderByCreatedAtDesc(
                user,
                StaffInvitationStatus.PENDING
        ).ifPresent(invitation -> {
            if (invitation.getExpiresAt().isAfter(Instant.now())) {
                throw conflict("This account already has a pending staff invitation");
            }
            invitation.setStatus(StaffInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            assignmentRepository.findAllByStaffInvitation(invitation).forEach(assignment -> {
                if (assignment.getStatus() == RoleAssignmentStatus.PENDING) {
                    assignment.setStatus(RoleAssignmentStatus.EXPIRED);
                    assignment.setChangedByUser(actor);
                    assignment.setUpdatedAt(Instant.now());
                    assignmentRepository.save(assignment);
                }
            });
        });

        StaffInvitation invitation = createInvitation(
                actor,
                user.getName(),
                user.getEmail(),
                roles,
                user,
                jobTitle,
                department,
                employeeNumber,
                workEmail,
                accessExpiresAt,
                reason
        );

        StaffProfile profile = existingProfile == null ? new StaffProfile() : existingProfile;
        profile.setUser(user);
        profile.setEmploymentStatus(StaffEmploymentStatus.PENDING);
        applyProfileMetadata(profile, invitation);
        staffProfileRepository.save(profile);

        for (Role role : roles) {
            UserRoleAssignment assignment = new UserRoleAssignment();
            assignment.setUser(user);
            assignment.setRole(role);
            assignment.setContext(AccessContext.STAFF);
            assignment.setStatus(RoleAssignmentStatus.PENDING);
            assignment.setGrantedByUser(actor);
            assignment.setReason(reason);
            assignment.setExpiresAt(accessExpiresAt);
            assignment.setStaffInvitation(invitation);
            assignmentRepository.save(assignment);
        }
        return invitation;
    }

    private StaffInvitation createInvitation(
            User actor,
            String name,
            String email,
            Set<Role> roles,
            User existingUser,
            String jobTitle,
            String department,
            String employeeNumber,
            String workEmail,
            Instant accessExpiresAt,
            String reason
    ) {
        String rawToken = tokenService.generate();
        StaffInvitation invitation = new StaffInvitation();
        invitation.setName(name);
        invitation.setEmail(email);
        invitation.setTokenHash(tokenService.hash(rawToken));
        invitation.setRoles(roles);
        invitation.setInvitedByUser(actor);
        invitation.setLinkedProductUser(existingUser);
        invitation.setJobTitle(jobTitle);
        invitation.setDepartment(department);
        invitation.setEmployeeNumber(normalizeOptional(employeeNumber));
        invitation.setWorkEmail(normalizeOptional(workEmail));
        invitation.setAccessExpiresAt(accessExpiresAt);
        invitation.setGrantReason(reason);
        invitation.setStatus(StaffInvitationStatus.PENDING);
        invitation.setExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS));
        StaffInvitation saved = invitationRepository.save(invitation);
        sendInvitation(saved, rawToken);
        return saved;
    }

    private void activateStaffAccess(User user, StaffInvitation invitation) {
        StaffProfile profile = staffProfileRepository.findByUser(user).orElseGet(StaffProfile::new);
        profile.setUser(user);
        profile.setEmploymentStatus(StaffEmploymentStatus.ACTIVE);
        profile.setActivatedAt(Instant.now());
        applyProfileMetadata(profile, invitation);
        staffProfileRepository.save(profile);

        List<UserRoleAssignment> assignments = assignmentRepository.findAllByStaffInvitation(invitation);
        if (assignments.isEmpty()) {
            for (Role role : invitation.getRoles()) {
                UserRoleAssignment assignment = new UserRoleAssignment();
                assignment.setUser(user);
                assignment.setRole(role);
                assignment.setContext(AccessContext.STAFF);
                assignment.setStatus(RoleAssignmentStatus.ACTIVE);
                assignment.setGrantedByUser(invitation.getInvitedByUser());
                assignment.setReason(invitation.getGrantReason());
                assignment.setExpiresAt(invitation.getAccessExpiresAt());
                assignment.setActivatedAt(Instant.now());
                assignment.setStaffInvitation(invitation);
                assignmentRepository.save(assignment);
            }
            return;
        }
        assignments.forEach(assignment -> {
            assignment.setStatus(RoleAssignmentStatus.ACTIVE);
            assignment.setActivatedAt(Instant.now());
            assignmentRepository.save(assignment);
        });
    }

    private void applyProfileMetadata(StaffProfile profile, StaffInvitation invitation) {
        profile.setJobTitle(invitation.getJobTitle());
        profile.setDepartment(invitation.getDepartment());
        profile.setEmployeeNumber(invitation.getEmployeeNumber());
        profile.setWorkEmail(invitation.getWorkEmail());
        profile.setGrantReason(invitation.getGrantReason());
        profile.setExpiresAt(invitation.getAccessExpiresAt());
        profile.setGrantedByUser(invitation.getInvitedByUser());
    }

    private void markAccepted(StaffInvitation invitation, User user) {
        invitation.setStatus(StaffInvitationStatus.ACCEPTED);
        invitation.setAcceptedUser(user);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
    }

    private StaffInvitation requireUsableToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw badRequest("Invitation token is required");
        }
        StaffInvitation invitation = invitationRepository
                .findByTokenHash(tokenService.hash(rawToken.trim()))
                .orElseThrow(() -> new AccessManagementException(
                        "Invitation is invalid or no longer available",
                        HttpStatus.NOT_FOUND
                ));
        if (invitation.getStatus() != StaffInvitationStatus.PENDING) {
            throw badRequest("Invitation is no longer available");
        }
        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(StaffInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new AccessManagementException("Invitation has expired", HttpStatus.GONE);
        }
        return invitation;
    }

    private StaffInvitation requireInvitation(Long id) {
        return invitationRepository.findById(id)
                .orElseThrow(() -> new AccessManagementException(
                        "Staff invitation not found",
                        HttpStatus.NOT_FOUND
                ));
    }

    private Set<Role> resolveRoles(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw badRequest("At least one staff role is required");
        }
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));
        if (roles.size() != roleIds.size()
                || roles.stream().anyMatch(role -> role.getAudience() != RoleAudience.PLATFORM_STAFF)) {
            throw badRequest("One or more selected staff roles are invalid");
        }
        return roles;
    }

    private void ensureNoPendingEmailInvitation(String email) {
        if (invitationRepository.existsByEmailIgnoreCaseAndStatus(email, StaffInvitationStatus.PENDING)) {
            throw conflict("A pending invitation already exists for this email");
        }
    }

    private void sendInvitation(StaffInvitation invitation, String rawToken) {
        String link = UriComponentsBuilder
                .fromUriString(frontendBaseDomain)
                .path("/auth/accept-invite")
                .queryParam("token", rawToken)
                .build()
                .encode()
                .toUriString();
        emailService.sendStaffInvitation(
                invitation.getEmail(),
                invitation.getName(),
                link,
                invitation.getExpiresAt()
        );
    }

    private String normalizeEmail(String value) {
        String email = normalizeRequired(value, "Email").toLowerCase(Locale.ROOT);
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw badRequest("Enter a valid email address");
        }
        return email;
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw badRequest(label + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AccessManagementException badRequest(String message) {
        return new AccessManagementException(message, HttpStatus.BAD_REQUEST);
    }

    private AccessManagementException conflict(String message) {
        return new AccessManagementException(message, HttpStatus.CONFLICT);
    }
}
