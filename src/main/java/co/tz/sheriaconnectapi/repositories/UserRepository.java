package co.tz.sheriaconnectapi.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import co.tz.sheriaconnectapi.model.Entities.User;

import java.util.Optional;
import co.tz.sheriaconnectapi.model.Enums.UserAccountType;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.authorities
            WHERE u.email = :email
            """)
    Optional<User> findByEmail(@Param("email") String email);

    boolean existsByEmail(String email);

    long countByAccountType(UserAccountType accountType);

    long countByAccountTypeAndActiveTrue(UserAccountType accountType);

    long countByAccountTypeAndLockedTrue(UserAccountType accountType);

    long countByLockedTrue();

    long countByRoles_Id(Long roleId);
    long countByRoles_IdAndActiveTrueAndLockedFalse(Long roleId);

    boolean existsByRoles_Id(Long roleId);

    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.authorities
            WHERE u.id = :id
            """)
    Optional<User> findDetailedById(@Param("id") Long id);
}
