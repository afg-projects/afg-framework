package io.github.afgprojects.framework.ai.core.api.pipeline;

import org.jspecify.annotations.NonNull;
import java.util.List;

public interface KnowledgeSearchClient {
    @NonNull List<SearchResult> search(@NonNull String query,
                                        @NonNull List<String> knowledgeIds,
                                        @NonNull SearchMode searchMode,
                                        double similarityThreshold,
                                        int topN);
}
