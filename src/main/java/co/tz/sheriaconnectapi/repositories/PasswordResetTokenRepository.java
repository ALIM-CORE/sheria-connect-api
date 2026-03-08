package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByUserId(Long userId);
}
