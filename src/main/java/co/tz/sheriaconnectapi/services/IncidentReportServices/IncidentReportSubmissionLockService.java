package co.tz.sheriaconnectapi.services.IncidentReportServices;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class IncidentReportSubmissionLockService {

    @PersistenceContext
    private EntityManager entityManager;

    public void lock(UUID submissionId) {
        long lockKey = submissionId.getMostSignificantBits()
                ^ Long.rotateLeft(submissionId.getLeastSignificantBits(), 1);
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(?1)")
                .setParameter(1, lockKey)
                .getSingleResult();
    }
}
