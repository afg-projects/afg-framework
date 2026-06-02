package io.github.afgprojects.framework.ai.core.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.KnowledgeSearchClient;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchMode;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchResult;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * No-op implementation of {@link KnowledgeSearchClient} that always returns empty results.
 *
 * <p>Used as a fallback when no knowledge search service is available,
 * ensuring the chat pipeline can still function without RAG support.
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class NoOpKnowledgeSearchClient implements KnowledgeSearchClient {

    @Override
    @NonNull
    public List<SearchResult> search(@NonNull String query,
                                     @NonNull List<String> knowledgeIds,
                                     @NonNull SearchMode searchMode,
                                     double similarityThreshold,
                                     int topN) {
        return List.of();
    }
}
