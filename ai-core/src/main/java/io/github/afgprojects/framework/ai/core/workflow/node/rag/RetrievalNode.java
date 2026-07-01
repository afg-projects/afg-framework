package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retrieval node - retrieves relevant documents from a knowledge base.
 *
 * <p>Searches a knowledge base for documents relevant to the given query.
 * Used in RAG (Retrieval-Augmented Generation) workflows to fetch
 * context for AI generation.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link KnowledgeBaseService} is a construction-time dependency; the
 * no-arg constructor leaves it null so the node degrades gracefully when
 * retrieval is not configured.</p>
 */
@Slf4j
public class RetrievalNode extends AbstractWorkflowNode<RetrievalNode.Params> {

    public static final String TYPE = "retrieval";

    /** Strongly-typed parameters for {@link RetrievalNode}. */
    public record Params(
            @Param(displayName = "Query", description = "Search query text", required = true)
            String query,
            @Param(displayName = "Knowledge base ID", description = "ID of the knowledge base to search")
            String knowledgeBaseId,
            @Param(displayName = "Top K", description = "Maximum number of results", defaultValue = "5")
            Integer topK,
            @Param(displayName = "Similarity threshold", description = "Minimum similarity score", defaultValue = "0.7")
            Double similarityThreshold
    ) {
        /** Effective topK, defaulting to 5. */
        public int effectiveTopK() {
            return topK == null ? 5 : topK;
        }

        /** Effective similarity threshold, defaulting to 0.7. */
        public double effectiveSimilarityThreshold() {
            return similarityThreshold == null ? 0.7 : similarityThreshold;
        }
    }

    /** Output descriptor for {@link RetrievalNode}. */
    public record Output(
            @Out(description = "Query") String query,
            @Out(description = "Result count") int resultCount,
            @Out(description = "Search results") Object results
    ) {}

    private final KnowledgeBaseService knowledgeBaseService;

    public RetrievalNode(String nodeId, KnowledgeBaseService knowledgeBaseService) {
        super(nodeId, TYPE, Params.class);
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public RetrievalNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.knowledgeBaseService = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String query = params.query();
        String knowledgeBaseId = params.knowledgeBaseId();
        int topK = params.effectiveTopK();
        double similarityThreshold = params.effectiveSimilarityThreshold();

        log.debug("RetrievalNode [{}] searching for: {}", getNodeId(), query);

        if (knowledgeBaseService == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("query", query);
            result.put("error", "No KnowledgeBaseService available - retrieval not configured");
            return result;
        }

        var searchResults = knowledgeBaseService.search(knowledgeBaseId, query, topK, similarityThreshold);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("resultCount", searchResults != null ? searchResults.size() : 0);
        result.put("results", searchResults);
        return result;
    }
}
