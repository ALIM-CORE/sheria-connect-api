package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import co.tz.sheriaconnectapi.model.Enums.IncidentType;
import co.tz.sheriaconnectapi.model.Enums.IncidentUrgency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "incident_reports",
        indexes = {
                @Index(name = "idx_incident_reports_case_number", columnList = "case_number", unique = true),
                @Index(name = "idx_incident_reports_status", columnList = "status"),
                @Index(name = "idx_incident_reports_reporter_user_id", columnList = "reporter_user_id"),
                @Index(name = "idx_incident_reports_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_number", nullable = false, unique = true, length = 32)
    private String caseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id")
    private User reporterUser;

    @Column(name = "tracking_token_hash", length = 128)
    private String trackingTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "anonymity_mode", nullable = false, length = 32)
    private AnonymityMode anonymityMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_type", nullable = false, length = 64)
    private IncidentType incidentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentUrgency urgency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private IncidentReportStatus status = IncidentReportStatus.SUBMITTED;

    @Column(length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "incident_date")
    private LocalDate incidentDate;

    @Column(name = "location_description", columnDefinition = "TEXT")
    private String locationDescription;

    @Column(length = 120)
    private String region;

    @Column(length = 120)
    private String district;

    @Column(length = 120)
    private String ward;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "pseudonym", length = 120)
    private String pseudonym;

    @Column(name = "contact_name", length = 160)
    private String contactName;

    @Column(name = "contact_email", length = 180)
    private String contactEmail;

    @Column(name = "contact_phone", length = 80)
    private String contactPhone;

    @Column(name = "matching_requested", nullable = false)
    private boolean matchingRequested = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "incidentReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<EvidenceFile> evidenceFiles = new ArrayList<>();

    @OneToMany(mappedBy = "incidentReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<CaseStatusHistory> statusHistory = new ArrayList<>();

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
