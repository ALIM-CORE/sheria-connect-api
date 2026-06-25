package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.StaffInvitation;
import co.tz.sheriaconnectapi.model.Enums.StaffInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import co.tz.sheriaconnectapi.model.Entities.User;

public interface StaffInvitationRepository extends JpaRepository<StaffInvitation, Long> {
    Optional<StaffInvitation> findByTokenHash(String tokenHash);
    List<StaffInvitation> findAllByOrderByCreatedAtDesc();
    boolean existsByEmailIgnoreCaseAndStatus(String email, StaffInvitationStatus status);
    boolean existsByLinkedProductUserAndStatus(User linkedProductUser, StaffInvitationStatus status);
    Optional<StaffInvitation> findFirstByLinkedProductUserAndStatusOrderByCreatedAtDesc(
            User linkedProductUser,
            StaffInvitationStatus status
    );
    long countByStatus(StaffInvitationStatus status);
}
