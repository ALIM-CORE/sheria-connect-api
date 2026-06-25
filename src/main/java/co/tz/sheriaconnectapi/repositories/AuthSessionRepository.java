package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.AuthSession;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    Optional<AuthSession> findBySessionId(String sessionId);

    @Modifying
    @Query("""
            UPDATE AuthSession session
               SET session.revoked = true
             WHERE session.user.id = :userId
               AND session.activeContext = :context
            """)
    void revokeByUserIdAndContext(
            @Param("userId") Long userId,
            @Param("context") AccessContext context
    );

    @Modifying
    @Query("UPDATE AuthSession session SET session.revoked = true WHERE session.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);
}
