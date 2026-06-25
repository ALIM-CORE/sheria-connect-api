package co.tz.sheriaconnectapi.services.AccessManagementServices;

import co.tz.sheriaconnectapi.model.Entities.AccessAuditEvent;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.repositories.AccessAuditEventRepository;
import org.springframework.stereotype.Service;
import co.tz.sheriaconnectapi.security.Access.SessionAuthenticationDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@Service
public class AccessAuditService {
    private final AccessAuditEventRepository repository;

    public AccessAuditService(AccessAuditEventRepository repository) {
        this.repository = repository;
    }

    public void log(
            User actor,
            User target,
            String action,
            String entityType,
            Object entityId,
            String details
    ) {
        AccessAuditEvent event = new AccessAuditEvent();
        event.setActorUser(actor);
        event.setTargetUser(target);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId == null ? null : entityId.toString());
        event.setDetails(details);
        event.setResult("SUCCESS");
        event.setCorrelationId(UUID.randomUUID().toString());
        repository.save(event);
    }

    public void log(
            User actor,
            User target,
            String action,
            String entityType,
            Object entityId,
            String reason,
            String beforeValue,
            String afterValue,
            String result,
            Authentication authentication,
            HttpServletRequest request
    ) {
        AccessAuditEvent event = new AccessAuditEvent();
        event.setActorUser(actor);
        event.setTargetUser(target);
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId == null ? null : entityId.toString());
        event.setReason(reason);
        event.setBeforeValue(beforeValue);
        event.setAfterValue(afterValue);
        event.setResult(result);
        event.setCorrelationId(UUID.randomUUID().toString());
        if (authentication != null
                && authentication.getDetails() instanceof SessionAuthenticationDetails details) {
            event.setSessionId(details.sessionId());
        }
        if (request != null) {
            String forwarded = request.getHeader("X-Forwarded-For");
            event.setIpAddress(forwarded == null
                    ? request.getRemoteAddr()
                    : forwarded.split(",")[0].trim());
        }
        repository.save(event);
    }
}
