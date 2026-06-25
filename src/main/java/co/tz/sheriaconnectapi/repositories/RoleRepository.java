package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import co.tz.sheriaconnectapi.model.Enums.RoleAudience;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    List<Role> findAllByOrderByDisplayNameAsc();

    List<Role> findByAudienceOrderByDisplayNameAsc(RoleAudience audience);
}
