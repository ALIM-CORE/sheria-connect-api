package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, Long> {
    Optional<AuthChallenge> findByTokenHash(String tokenHash);
}
