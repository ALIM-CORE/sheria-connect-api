package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.User;
import co.tz.sheriaconnectapi.model.Enums.StoryModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublicStoryRepository extends JpaRepository<PublicStory, Long>, JpaSpecificationExecutor<PublicStory> {
    Optional<PublicStory> findByPublicId(String publicId);
    List<PublicStory> findByAuthorUserOrderByCreatedAtDesc(User authorUser);
    boolean existsByPublicId(String publicId);
    long countByModerationStatus(StoryModerationStatus moderationStatus);
}
