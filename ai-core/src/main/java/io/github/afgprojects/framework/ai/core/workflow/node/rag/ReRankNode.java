package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Re-rank node - re-ranks search results by relevance.
 *
 * <p>Takes a list of search results and re-ranks them by relevance to the
 * query, typically using a cross-encoder or LLM-based scoring approach.
 * Improves retrieval quality by applying more sophisticated relevance
 * scoring than initial vector similarity.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link AfgChatClient} is a construction-time dependency; the no-arg
 * constructor leaves it null (score-based re-ranking still works without it).</p>
 *
 * <p><strong>Alpha feature:</strong> LLM-based re-ranking requires an AfgChatClient.
 * Current implementation supports score-based re-ranking using existing relevance scores.</p>
 */
@Slf4j
public class ReRankNode extends AbstractWorkflowNode<ReRankNode.Params> {

    public static final String TYPE = "re-rank";

    /** Strongly-typed parameters for {@link ReRankNode}. */
    public record Params(
            @Param(displayName = "Query", description = "The original search query", required = true)
            String query,
            @Param(displayName = "Results", description = "List of search results to re-rank", required = true)
            List<Object> results,
            @Param(displayName = "Top K", description = "Number of top results to return", defaultValue = "5")
            Integer topK,
            @Param(displayName = "Method", description = "Re-ranking method: \"llm\" or \"score\"", defaultValue = "score")
            String method
    ) {
        /** Effective topK, defaulting to 5. */
        public int effectiveTopK() {
            return topK == null ? 5 : topK;
        }

        /** Effective method, defaulting to "score". */
        public String effectiveMethod() {
            return method == null || method.isBlank() ? "score" : method;
        }
    }

    /** Output descriptor for {@link ReRankNode}. */
    public record Output(
            @Out(description = "Query") String query,
            @Out(description = "Result count") int resultCount,
            @Out(description = "Method") String method,
            @Out(description = "Re-ranked results") List<Object> results
    ) {}

    private final AfgChatClient chatClient;

    public ReRankNode(String nodeId, AfgChatClient chatClient) {
        super(nodeId, TYPE, Params.class);
        this.chatClient = chatClient;
    }

    public ReRankNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.chatClient = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String query = params.query();
        List<Object> results = params.results();
        if (results == null) {
            throw new IllegalArgumentException("Required parameter 'results' is missing");
        }

        int topK = params.effectiveTopK();
        String method = params.effectiveMethod();

        log.debug("ReRankNode [{}] re-ranking {} results for query: {}", getNodeId(), results.size(), truncate(query, 100));

        // Score-based re-ranking: sort by existing "score" or "similarity" field
        List<Object> ranked = results.stream()
                .sorted((a, b) -> {
                    double scoreA = extractScore(a);
                    double scoreB = extractScore(b);
                    return Double.compare(scoreB, scoreA); // descending
                })
                .limit(topK)
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("query", query);
        result.put("resultCount", ranked.size());
        result.put("method", method);
        result.put("results", ranked);
        return result;
    }

    @SuppressWarnings("unchecked")
    private double extractScore(Object item) {
        if (item instanceof Map<?, ?> map) {
            Object score = ((Map<String, Object>) map).get("score");
            if (score == null) score = ((Map<String, Object>) map).get("similarity");
            if (score instanceof Number num) return num.doubleValue();
        }
        return 0.0;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
