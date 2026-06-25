package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.StaffEmploymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
public class StaffProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 24)
    private StaffEmploymentStatus employmentStatus = StaffEmploymentStatus.PENDING;

    @Column(name = "job_title", length = 160)
    private String jobTitle;

    @Column(length = 160)
    private String department;

    @Column(name = "employee_number", length = 100)
    private String employeeNumber;

    @Column(name = "work_email", length = 255)
    private String workEmail;

    @Column(name = "grant_reason", nullable = false, columnDefinition = "TEXT")
    private String grantReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by_user_id")
    private User grantedByUser;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
