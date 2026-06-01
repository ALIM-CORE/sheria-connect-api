package co.tz.sheriaconnectapi.model.DTOs;

import co.tz.sheriaconnectapi.model.Entities.LegalKnowledgeArticle;

import java.time.Instant;

public record KnowledgeArticleResponse(
        Long id,
        String slug,
        String title,
        String category,
        String summary,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
    public KnowledgeArticleResponse(LegalKnowledgeArticle article, boolean includeBody) {
        this(
                article.getId(),
                article.getSlug(),
                article.getTitle(),
                article.getCategory(),
                article.getSummary(),
                includeBody ? article.getBody() : null,
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }
}
