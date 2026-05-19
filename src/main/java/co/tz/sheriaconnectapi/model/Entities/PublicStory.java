package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.AnonymityMode;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "public_stories",
        indexes = {
                @Index(name = "idx_public_stories_public_id", columnList = "public_id", unique = true),
                @Index(name = "idx_public_stories_status", columnList = "moderation_status"),
                @Index(name = "idx_public_stories_category", columnList = "category"),
                @Index(name = "idx_public_stories_region", columnList = "region"),
                @Index(name = "idx_public_stories_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PublicStory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 64)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id")
    private User authorUser;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 120)
    private String region;

    @Column(length = 120)
    private String district;

    @Enumerated(EnumType.STRING)
    @Column(name = "anonymity_mode", nullable = false, length = 32)
    private AnonymityMode anonymityMode = AnonymityMode.FULLY_ANONYMOUS;

    @Column(name = "display_name", length = 120)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false, length = 32)
    private StoryModerationStatus moderationStatus = StoryModerationStatus.PENDING_REVIEW;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "story", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoryReaction> reactions = new ArrayList<>();

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
