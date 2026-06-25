package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.MfaRecoveryCode;
import co.tz.sheriaconnectapi.model.Entities.StaffMfaCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {
    List<MfaRecoveryCode> findAllByCredentialAndUsedAtIsNull(StaffMfaCredential credential);

    void deleteAllByCredential(StaffMfaCredential credential);
}
