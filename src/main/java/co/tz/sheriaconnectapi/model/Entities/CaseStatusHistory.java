package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.IncidentReportStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "case_status_history",
        indexes = {
                @Index(name = "idx_case_status_history_report_id", columnList = "incident_report_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CaseStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport incidentReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 32)
    private IncidentReportStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 32)
    private IncidentReportStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id")
    private User changedByUser;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
