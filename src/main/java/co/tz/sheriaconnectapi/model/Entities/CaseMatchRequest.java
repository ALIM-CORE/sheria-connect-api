package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.MatchingRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "case_match_requests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_case_match_requests_report_provider",
                        columnNames = {"incident_report_id", "provider_profile_id"}
                )
        },
        indexes = {
                @Index(name = "idx_case_match_requests_report_id", columnList = "incident_report_id"),
                @Index(name = "idx_case_match_requests_provider_id", columnList = "provider_profile_id"),
                @Index(name = "idx_case_match_requests_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CaseMatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport incidentReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_profile_id", nullable = false)
    private ProviderProfile providerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MatchingRequestStatus status = MatchingRequestStatus.REQUESTED;

    @Column(nullable = false)
    private int score;

    @Column(name = "score_breakdown", columnDefinition = "TEXT")
    private String scoreBreakdown;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
