package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import co.tz.sheriaconnectapi.model.Enums.AccessContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserAndContextOrderByCreatedAtDesc(
            User user,
            AccessContext context
    );

    long countByUserAndContextAndReadAtIsNull(User user, AccessContext context);
}
