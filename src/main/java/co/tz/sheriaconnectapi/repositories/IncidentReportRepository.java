package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    boolean existsByCaseNumber(String caseNumber);

    Optional<IncidentReport> findByCaseNumber(String caseNumber);

    List<IncidentReport> findByReporterUserOrderByCreatedAtDesc(User reporterUser);

    List<IncidentReport> findAllByOrderByCreatedAtDesc();
}
