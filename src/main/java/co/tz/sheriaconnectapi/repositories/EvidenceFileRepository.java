package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.EvidenceFile;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, Long> {
    List<EvidenceFile> findByIncidentReportOrderByCreatedAtDesc(IncidentReport incidentReport);

    Optional<EvidenceFile> findByIdAndIncidentReport(Long id, IncidentReport incidentReport);
}
