package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.StoryContentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryContentReportRepository extends JpaRepository<StoryContentReport, Long> {
}
