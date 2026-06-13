package io.github.afgprojects.framework.ai.core.workflow.node.ai;

import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Embedding node - converts text to vector embeddings.
 *
 * <p>Uses an {@link AfgEmbeddingClient} to convert text input into vector
 * embeddings, which can then be used for similarity search or storage.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code texts} (required) - list of text strings to embed, or a single string</li>
 *   <li>{@code modelName} (optional) - name of the embedding model to use</li>
 * </ul>
 */
@Slf4j
public class AiEmbeddingNode extends AbstractWorkflowNode {

    public static final String TYPE = "ai-embedding";

    private final AfgEmbeddingClient embeddingClient;

    public AiEmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient) {
        super(nodeId, TYPE);
        this.embeddingClient = embeddingClient;
    }

    public AiEmbeddingNode(String nodeId) {
        super(nodeId, TYPE);
        this.embeddingClient = null;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        Object textInput = params.get("texts");
        if (textInput == null) {
            textInput = params.get("text");
        }
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

        String modelName = getParam(params, "modelName", null);

        log.debug("AiEmbeddingNode [{}] embedding {} texts", getNodeId(), texts.size());

        if (embeddingClient == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("texts", texts);
            result.put("error", "No AfgEmbeddingClient available - embedding not configured");
            return result;
        }

        AfgEmbeddingClient client = embeddingClient;
        if (modelName != null) {
            client = client.withModel(modelName);
        }

        List<float[]> embeddings = client.embed(texts);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", embeddings.size());
        result.put("dimensions", embeddings.isEmpty() ? 0 : embeddings.get(0).length);

        // Convert float[] to List<Float> for JSON serialization
        List<List<Float>> embeddingLists = new ArrayList<>(embeddings.size());
        for (float[] embedding : embeddings) {
            List<Float> list = new ArrayList<>(embedding.length);
            for (float v : embedding) {
                list.add(v);
            }
            embeddingLists.add(list);
        }
        result.put("embeddings", embeddingLists);

        return result;
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}
