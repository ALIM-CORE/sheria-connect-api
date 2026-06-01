package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.CaseMessageSenderRole;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "case_messages",
        indexes = {
                @Index(name = "idx_case_messages_report_id", columnList = "incident_report_id"),
                @Index(name = "idx_case_messages_match_request_id", columnList = "case_match_request_id"),
                @Index(name = "idx_case_messages_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class CaseMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport incidentReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_match_request_id")
    private CaseMatchRequest caseMatchRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User senderUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 32)
    private CaseMessageSenderRole senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
