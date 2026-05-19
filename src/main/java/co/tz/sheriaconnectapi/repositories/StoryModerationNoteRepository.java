package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.model.Entities.StoryModerationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryModerationNoteRepository extends JpaRepository<StoryModerationNote, Long> {
    List<StoryModerationNote> findByStoryOrderByCreatedAtDesc(PublicStory story);
}
