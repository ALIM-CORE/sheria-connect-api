package co.tz.sheriaconnectapi.model.Entities;

import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ClientType clientType;

    private Instant expiryDate;

    private boolean revoked = false;

    private Instant createdAt = Instant.now();
}
