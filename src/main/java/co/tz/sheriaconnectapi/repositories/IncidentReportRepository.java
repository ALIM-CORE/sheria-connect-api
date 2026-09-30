package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    boolean existsByCaseNumber(String caseNumber);

    Optional<IncidentReport> findByCaseNumber(String caseNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from IncidentReport report where report.caseNumber = :caseNumber")
    Optional<IncidentReport> findByCaseNumberForUpdate(@Param("caseNumber") String caseNumber);

    Optional<IncidentReport> findBySubmissionId(UUID submissionId);

    List<IncidentReport> findByReporterUserOrderByCreatedAtDesc(User reporterUser);

    List<IncidentReport> findAllByOrderByCreatedAtDesc();

    long countByReporterUser(User reporterUser);
}
