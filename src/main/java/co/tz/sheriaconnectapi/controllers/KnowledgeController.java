package co.tz.sheriaconnectapi.controllers;

import co.tz.sheriaconnectapi.model.DTOs.KnowledgeArticleResponse;
import co.tz.sheriaconnectapi.services.KnowledgeServices.GetKnowledgeArticleService;
import co.tz.sheriaconnectapi.services.KnowledgeServices.ListKnowledgeArticlesService;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/knowledge/articles")
public class KnowledgeController {

    private final ListKnowledgeArticlesService listKnowledgeArticlesService;
    private final GetKnowledgeArticleService getKnowledgeArticleService;

    public KnowledgeController(
            ListKnowledgeArticlesService listKnowledgeArticlesService,
            GetKnowledgeArticleService getKnowledgeArticleService
    ) {
        this.listKnowledgeArticlesService = listKnowledgeArticlesService;
        this.getKnowledgeArticleService = getKnowledgeArticleService;
    }

    @GetMapping
    public ResponseEntity<StandardResponse<List<KnowledgeArticleResponse>>> list(
            @RequestParam(required = false) String category
    ) {
        return listKnowledgeArticlesService.execute(category);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<StandardResponse<KnowledgeArticleResponse>> get(
            @PathVariable String slug
    ) {
        return getKnowledgeArticleService.execute(slug);
    }
}
