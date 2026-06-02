package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchMode;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * REST-based implementation of {@link KnowledgeSearchClient} that calls a remote knowledge service.
 *
 * <p>Uses Spring's {@link RestClient} to communicate with the knowledge service API.
 * The service URL is configurable via properties.
 *
 * <p>Example configuration:
 * <pre>{@code
 * afg:
 *   ai:
 *     pipeline:
 *       knowledge-search:
 *         base-url: http://afg-ai-services-ai-knowledge
 *         search-path: /knowledge-api/v1/search
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class RestKnowledgeSearchClient implements KnowledgeSearchClient {

    private final RestClient restClient;

    @Override
    @NonNull
    public List<SearchResult> search(@NonNull String query,
                                     @NonNull List<String> knowledgeIds,
                                     @NonNull SearchMode searchMode,
                                     double similarityThreshold,
                                     int topN) {
        try {
            return restClient.post()
                .uri("/knowledge-api/v1/search")
                .body(Map.of(
                    "query", query,
                    "knowledgeIds", knowledgeIds,
                    "searchMode", searchMode.name(),
                    "similarityThreshold", similarityThreshold,
                    "topN", topN
                ))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.warn("Knowledge search request failed: {}", e.getMessage());
            return List.of();
        }
    }
}
