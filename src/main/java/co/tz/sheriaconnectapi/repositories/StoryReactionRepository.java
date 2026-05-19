package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryReaction;
import co.tz.sheriaconnectapi.model.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoryReactionRepository extends JpaRepository<StoryReaction, Long> {
    Optional<StoryReaction> findByStoryAndUser(PublicStory story, User user);
    long countByStory(PublicStory story);
    void deleteByStoryAndUser(PublicStory story, User user);
}
