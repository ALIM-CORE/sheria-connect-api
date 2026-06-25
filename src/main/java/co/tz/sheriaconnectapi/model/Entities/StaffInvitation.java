package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "staff_invitations")
@Getter
@Setter
public class StaffInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StaffInvitationStatus status = StaffInvitationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_user_id")
    private User invitedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepted_user_id")
    private User acceptedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_product_user_id")
    private User linkedProductUser;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "staff_invitation_roles",
            joinColumns = @JoinColumn(name = "invitation_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "job_title", length = 160)
    private String jobTitle;

    @Column(length = 160)
    private String department;

    @Column(name = "employee_number", length = 100)
    private String employeeNumber;

    @Column(name = "work_email", length = 255)
    private String workEmail;

    @Column(name = "grant_reason", columnDefinition = "TEXT")
    private String grantReason;

    @Column(name = "access_expires_at")
    private Instant accessExpiresAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
