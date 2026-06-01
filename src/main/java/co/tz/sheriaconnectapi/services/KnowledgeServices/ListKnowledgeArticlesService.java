package co.tz.sheriaconnectapi.services.KnowledgeServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.KnowledgeArticleResponse;
import co.tz.sheriaconnectapi.repositories.LegalKnowledgeArticleRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListKnowledgeArticlesService implements Query<String, List<KnowledgeArticleResponse>> {

    private final LegalKnowledgeArticleRepository articleRepository;

    public ListKnowledgeArticlesService(LegalKnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<List<KnowledgeArticleResponse>>> execute(String category) {
        List<KnowledgeArticleResponse> articles = (
                category == null || category.isBlank()
                        ? articleRepository.findByPublishedTrueOrderBySortOrderAscTitleAsc()
                        : articleRepository.findByPublishedTrueAndCategoryIgnoreCaseOrderBySortOrderAscTitleAsc(
                                category.trim()
                        )
        ).stream()
                .map(article -> new KnowledgeArticleResponse(article, false))
                .toList();

        return ResponseUtil.success(articles, "Knowledge articles retrieved", HttpStatus.OK);
    }
}
