package co.tz.sheriaconnectapi.services.StoryServices;

import co.tz.sheriaconnectapi.abstractions.Query;
import co.tz.sheriaconnectapi.model.DTOs.StorySearchInput;
import co.tz.sheriaconnectapi.model.DTOs.StorySummaryResponse;
import co.tz.sheriaconnectapi.model.Entities.PublicStory;
import co.tz.sheriaconnectapi.utils.ResponseUtil;
import co.tz.sheriaconnectapi.utils.StandardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminListStoriesService implements Query<StorySearchInput, List<StorySummaryResponse>> {

    private final EntityManager entityManager;
    private final StoryResponseFactory storyResponseFactory;

    public AdminListStoriesService(
            EntityManager entityManager,
            StoryResponseFactory storyResponseFactory
    ) {
        this.entityManager = entityManager;
        this.storyResponseFactory = storyResponseFactory;
    }

    @Override
    public ResponseEntity<StandardResponse<List<StorySummaryResponse>>> execute(StorySearchInput input) {
        List<StorySummaryResponse> stories = search(input)
                .stream()
                .map(story -> storyResponseFactory.summary(story, null))
                .toList();

        return ResponseUtil.success(stories, "Stories retrieved successfully", HttpStatus.OK);
    }

    private List<PublicStory> search(StorySearchInput input) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<PublicStory> criteriaQuery = criteriaBuilder.createQuery(PublicStory.class);
        Root<PublicStory> story = criteriaQuery.from(PublicStory.class);
        List<Predicate> predicates = new ArrayList<>();

        if (input.status() != null) {
            predicates.add(criteriaBuilder.equal(story.get("moderationStatus"), input.status()));
        }
        addTextFilter(predicates, criteriaBuilder, story, "category", input.category());
        addTextFilter(predicates, criteriaBuilder, story, "region", input.region());
        addTextFilter(predicates, criteriaBuilder, story, "district", input.district());

        criteriaQuery
                .select(story)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.desc(story.get("createdAt")));

        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    private void addTextFilter(
            List<Predicate> predicates,
            CriteriaBuilder criteriaBuilder,
            Root<PublicStory> story,
            String field,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        predicates.add(criteriaBuilder.equal(
                criteriaBuilder.lower(story.get(field).as(String.class)),
                value.trim().toLowerCase()
        ));
    }
}
