package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    List<UserNotification> findByUserOrderByCreatedAtDesc(User user);

    long countByUserAndReadAtIsNull(User user);
}
