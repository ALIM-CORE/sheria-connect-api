package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.AccessAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessAuditEventRepository extends JpaRepository<AccessAuditEvent, Long> {
}
