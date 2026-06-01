package co.tz.sheriaconnectapi.services.KnowledgeServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.exceptions.KnowledgeArticleNotFoundException;
import co.tz.sheriaconnectapi.model.DTOs.KnowledgeArticleResponse;
import co.tz.sheriaconnectapi.repositories.LegalKnowledgeArticleRepository;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class GetKnowledgeArticleService implements Query<String, KnowledgeArticleResponse> {

    private final LegalKnowledgeArticleRepository articleRepository;

    public GetKnowledgeArticleService(LegalKnowledgeArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public ResponseEntity<StandardResponse<KnowledgeArticleResponse>> execute(String slug) {
        return ResponseUtil.success(
                articleRepository.findBySlugAndPublishedTrue(slug)
                        .map(article -> new KnowledgeArticleResponse(article, true))
                        .orElseThrow(KnowledgeArticleNotFoundException::new),
                "Knowledge article retrieved",
                HttpStatus.OK
        );
    }
}
