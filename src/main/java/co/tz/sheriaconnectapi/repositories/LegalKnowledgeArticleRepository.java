package co.tz.sheriaconnectapi.repositories;

import co.tz.sheriaconnectapi.model.Entities.LegalKnowledgeArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LegalKnowledgeArticleRepository extends JpaRepository<LegalKnowledgeArticle, Long> {
    List<LegalKnowledgeArticle> findByPublishedTrueOrderBySortOrderAscTitleAsc();

    List<LegalKnowledgeArticle> findByPublishedTrueAndCategoryIgnoreCaseOrderBySortOrderAscTitleAsc(
            String category
    );

    Optional<LegalKnowledgeArticle> findBySlugAndPublishedTrue(String slug);
}
