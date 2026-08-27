package co.tz.sheriaconnectapi.exceptions;

public class KnowledgeArticleNotFoundException extends DomainException {
    public KnowledgeArticleNotFoundException() {
        super(ErrorMessages.KNOWLEDGE_ARTICLE_NOT_FOUND);
    }
}
