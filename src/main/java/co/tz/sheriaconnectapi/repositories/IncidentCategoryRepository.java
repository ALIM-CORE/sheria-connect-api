package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.IncidentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, String> {
    List<IncidentCategory> findBySelectableTrueOrderBySortOrderAscCodeAsc();

    List<IncidentCategory> findAllByOrderBySortOrderAscCodeAsc();
}
