package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code query} (required) - the original search query</li>
 *   <li>{@code results} (required) - list of search results to re-rank</li>
 *   <li>{@code topK} (optional) - number of top results to return, defaults to 5</li>
 *   <li>{@code method} (optional) - re-ranking method: "llm" or "score", defaults to "score"</li>
 * </ul>
 *
 * <p><strong>Alpha feature:</strong> LLM-based re-ranking requires an AfgChatClient.
 * Current implementation supports score-based re-ranking using existing relevance scores.</p>
 */
@Slf4j
public class ReRankNode extends AbstractWorkflowNode {

    public static final String TYPE = "re-rank";

    private final AfgChatClient chatClient;

    public ReRankNode(String nodeId, AfgChatClient chatClient) {
        super(nodeId, TYPE);
        this.chatClient = chatClient;
    }

    public ReRankNode(String nodeId) {
        super(nodeId, TYPE);
        this.chatClient = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String query = getRequiredParam(params, "query");
        List<Object> results = (List<Object>) params.get("results");
        if (results == null) {
            throw new IllegalArgumentException("Required parameter 'results' is missing");
        }

        int topK = getIntParam(params, "topK", 5);
        String method = getParam(params, "method", "score");

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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.intValue();
        return Integer.parseInt(value.toString());
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
