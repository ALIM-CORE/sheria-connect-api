package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.AdminCaseNote;
import co.tz.sheriaconnectapi.model.Entities.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminCaseNoteRepository extends JpaRepository<AdminCaseNote, Long> {
    List<AdminCaseNote> findByIncidentReportOrderByCreatedAtDesc(IncidentReport incidentReport);
}
