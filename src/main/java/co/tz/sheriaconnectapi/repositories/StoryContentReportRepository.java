package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.StoryContentReport;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryContentReportRepository extends JpaRepository<StoryContentReport, Long> {
    List<StoryContentReport> findByStoryOrderByCreatedAtDesc(PublicStory story);

    long countByStory(PublicStory story);

    @Query("select count(distinct report.story.id) from StoryContentReport report")
    long countDistinctReportedStories();
}
