package co.tz.sheriaconnectapi.model.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "user_account_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_account_links_product", columnNames = "product_user_id"),
                @UniqueConstraint(name = "uk_user_account_links_pair", columnNames = {"staff_user_id", "product_user_id"})
        }
)
@Getter
@Setter
public class UserAccountLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_user_id", nullable = false)
    private User staffUser;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_user_id", nullable = false, unique = true)
    private User productUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by_user_id")
    private User linkedByUser;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
