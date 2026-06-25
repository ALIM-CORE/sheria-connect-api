package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.StaffMfaCredential;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffMfaCredentialRepository extends JpaRepository<StaffMfaCredential, Long> {
    Optional<StaffMfaCredential> findByUser(User user);
}
