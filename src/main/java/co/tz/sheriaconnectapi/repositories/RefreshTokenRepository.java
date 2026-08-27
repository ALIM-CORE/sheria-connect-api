package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUserId(Long userId);

    void deleteAllByAuthSession_Id(Long authSessionId);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revoked = true,
                   rt.revokedAt = CURRENT_TIMESTAMP
             WHERE rt.user.id = :userId
               AND rt.revoked = false
            """)
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revoked = true,
                   rt.revokedAt = CURRENT_TIMESTAMP
             WHERE rt.user.id = :userId
               AND rt.authSession.activeContext = :context
               AND rt.revoked = false
            """)
    void revokeAllByUserIdAndContext(
            @Param("userId") Long userId,
            @Param("context") AccessContext context
    );

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.email = :email")
    void deleteAllByUserEmail(@Param("email") String email);
}
