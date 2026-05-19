package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryBookmark;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryBookmarkRepository extends JpaRepository<StoryBookmark, Long> {
    Optional<StoryBookmark> findByStoryAndUser(PublicStory story, User user);
    List<StoryBookmark> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByStoryAndUser(PublicStory story, User user);
    void deleteByStoryAndUser(PublicStory story, User user);
}
