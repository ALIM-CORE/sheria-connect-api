package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.model.Enums.StoryReactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "story_reactions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_story_reactions_story_user", columnNames = {"story_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_story_reactions_story_id", columnList = "story_id"),
                @Index(name = "idx_story_reactions_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StoryReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private PublicStory story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction_type", nullable = false, length = 32)
    private StoryReactionType reactionType = StoryReactionType.SOLIDARITY;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
