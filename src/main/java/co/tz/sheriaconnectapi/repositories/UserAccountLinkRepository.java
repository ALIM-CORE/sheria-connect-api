package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Entities.UserAccountLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAccountLinkRepository extends JpaRepository<UserAccountLink, Long> {
    Optional<UserAccountLink> findByProductUser(User productUser);
    List<UserAccountLink> findAllByStaffUserOrderByCreatedAtAsc(User staffUser);
    boolean existsByProductUser(User productUser);
}
