package co.tz.sheriaconnectapi.model.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "incident_categories")
@Getter
@Setter
@NoArgsConstructor
public class IncidentCategory {

    @Id
    @Column(length = 64, nullable = false, updatable = false)
    private String code;

    @Column(name = "name_en", nullable = false, length = 180)
    private String nameEn;

    @Column(name = "name_sw", length = 180)
    private String nameSw;

    @Column(name = "body_en", columnDefinition = "TEXT")
    private String bodyEn;

    @Column(name = "body_sw", columnDefinition = "TEXT")
    private String bodySw;

    @Column(nullable = false)
    private boolean selectable;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
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
