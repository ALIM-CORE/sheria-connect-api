package co.tz.sheriaconnectapi.security.Access;

import co.tz.sheriaconnectapi.exceptions.InvalidClientTypeException;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;
import co.tz.sheriaconnectapi.security.Jwt.ClientType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AccessContextResolver {
    public ClientType clientType(HttpServletRequest request) {
        String value = request.getHeader("X-Client-Type");
        if (value == null || value.isBlank()) {
            return ClientType.WEB;
        }
        try {
            return ClientType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidClientTypeException();
        }
    }

    public AccessContext context(HttpServletRequest request, ClientType clientType, User user) {
        String value = request.getHeader("X-Active-Context");
        if (value != null && !value.isBlank()) {
            try {
                AccessContext context = AccessContext.valueOf(value.trim().toUpperCase(Locale.ROOT));
                validateClientContext(clientType, context);
                return context;
            } catch (IllegalArgumentException exception) {
                throw new InvalidClientTypeException();
            }
        }

        if (clientType == ClientType.WEB) {
            return AccessContext.STAFF;
        }
        return user.getAccountType() == UserAccountType.PROVIDER
                ? AccessContext.PROVIDER
                : AccessContext.CITIZEN;
    }

    private void validateClientContext(ClientType clientType, AccessContext context) {
        if (clientType == ClientType.WEB && context != AccessContext.STAFF) {
            throw new InvalidClientTypeException();
        }
        if (clientType == ClientType.MOBILE && context == AccessContext.STAFF) {
            throw new InvalidClientTypeException();
        }
    }

    public String audience(AccessContext context) {
        return switch (context) {
            case CITIZEN -> "citizen-app";
            case PROVIDER -> "provider-app";
            case STAFF -> "admin-portal";
        };
    }
}
