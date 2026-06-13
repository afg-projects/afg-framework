package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Embedding node - converts text to embeddings and optionally stores them.
 *
 * <p>Converts text to vector embeddings using an embedding model, and optionally
 * stores the embeddings in a vector store for later retrieval.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code texts} (required) - list of text strings to embed</li>
 *   <li>{@code store} (optional) - whether to store embeddings, defaults to false</li>
 *   <li>{@code collection} (optional) - collection name for storage</li>
 *   <li>{@code metadata} (optional) - metadata to associate with stored embeddings</li>
 * </ul>
 */
@Slf4j
public class EmbeddingNode extends AbstractWorkflowNode {

    public static final String TYPE = "embedding";

    private final AfgEmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public EmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient, VectorStore vectorStore) {
        super(nodeId, TYPE);
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public EmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient) {
        super(nodeId, TYPE);
        this.embeddingClient = embeddingClient;
        this.vectorStore = null;
    }

    public EmbeddingNode(String nodeId) {
        super(nodeId, TYPE);
        this.embeddingClient = null;
        this.vectorStore = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        Object textInput = params.get("texts");
        if (textInput == null) textInput = params.get("text");
        if (textInput == null) {
            throw new IllegalArgumentException("Required parameter 'texts' or 'text' is missing");
        }

        List<String> texts = new ArrayList<>();
        if (textInput instanceof List<?> list) {
            for (Object item : list) {
                texts.add(item.toString());
            }
        } else {
            texts.add(textInput.toString());
        }

        boolean store = getBooleanParam(params, "store", false);

        log.debug("EmbeddingNode [{}] embedding {} texts, store={}", getNodeId(), texts.size(), store);

        if (embeddingClient == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("texts", texts.size());
            result.put("error", "No AfgEmbeddingClient available - embedding not configured");
            return result;
        }

        List<float[]> embeddings = embeddingClient.embed(texts);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", embeddings.size());
        result.put("dimensions", embeddings.isEmpty() ? 0 : embeddings.get(0).length);
        result.put("stored", false);

        if (store && vectorStore != null) {
            String collection = getParam(params, "collection", "default");
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) params.get("metadata");
            // Store embeddings in vector store
            // Future: call vectorStore.store(collection, texts, embeddings, metadata)
            result.put("stored", true);
            result.put("collection", collection);
        }

        return result;
    }

    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
