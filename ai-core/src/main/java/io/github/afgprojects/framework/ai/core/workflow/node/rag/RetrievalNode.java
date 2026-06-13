package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
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
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code query} (required) - search query text</li>
 *   <li>{@code knowledgeBaseId} (optional) - ID of the knowledge base to search</li>
 *   <li>{@code topK} (optional) - maximum number of results, defaults to 5</li>
 *   <li>{@code similarityThreshold} (optional) - minimum similarity score, defaults to 0.7</li>
 * </ul>
 */
@Slf4j
public class RetrievalNode extends AbstractWorkflowNode {

    public static final String TYPE = "retrieval";

    private final KnowledgeBaseService knowledgeBaseService;

    public RetrievalNode(String nodeId, KnowledgeBaseService knowledgeBaseService) {
        super(nodeId, TYPE);
        this.knowledgeBaseService = knowledgeBaseService;
    }

    public RetrievalNode(String nodeId) {
        super(nodeId, TYPE);
        this.knowledgeBaseService = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        String query = getRequiredParam(params, "query");
        String knowledgeBaseId = getParam(params, "knowledgeBaseId", null);
        int topK = getIntParam(params, "topK", 5);
        double similarityThreshold = getDoubleParam(params, "similarityThreshold", 0.7);

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

    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number num) return num.doubleValue();
        return Double.parseDouble(value.toString());
    }
}
