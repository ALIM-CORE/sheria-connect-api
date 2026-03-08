package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUserId(Long userId);
}
