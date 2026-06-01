package co.tz.sheriaconnectapi.exceptions;

public class KnowledgeArticleNotFoundException extends RuntimeException {
    public KnowledgeArticleNotFoundException() {
        super(ErrorMessages.KNOWLEDGE_ARTICLE_NOT_FOUND.getMessage());
    }
}
