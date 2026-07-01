package io.github.afgprojects.framework.ai.core.workflow.node.ai;

import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * embeddings, which can then be used for similarity search or storage. Accepts
 * either a list of texts or a single text string.</p>
 *
 * <p>Parameters are declared on {@link Params}; the embedding client is a
 * construction-time dependency supplied by the node factory.</p>
 */
@Slf4j
public class AiEmbeddingNode extends AbstractWorkflowNode<AiEmbeddingNode.Params> {

    public static final String TYPE = "ai-embedding";

    /** Strongly-typed parameters for {@link AiEmbeddingNode}. */
    public record Params(
            @Param(displayName = "Texts to embed", description = "List of text strings to embed", required = true)
            List<String> texts,
            @Param(displayName = "Text", description = "Single text to embed (used when texts is absent)")
            String text,
            @Param(displayName = "Embedding model", description = "Name of the embedding model to use")
            String modelName
    ) {
        /** Normalize inputs into a non-null list of texts (texts first, then [text]). */
        public List<String> effectiveTexts() {
            if (texts != null && !texts.isEmpty()) {
                return texts;
            }
            return text != null ? List.of(text) : List.of();
        }
    }

    /** Output descriptor for {@link AiEmbeddingNode}. */
    public record Output(
            @Out(description = "Embedding vectors") List<List<Float>> embeddings,
            @Out(description = "Embedding count") int count,
            @Out(description = "Vector dimensions") int dimensions
    ) {}

    private final AfgEmbeddingClient embeddingClient;

    public AiEmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient) {
        super(nodeId, TYPE, Params.class);
        this.embeddingClient = embeddingClient;
    }

    public AiEmbeddingNode(String nodeId) {
        this(nodeId, null);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        List<String> texts = params.effectiveTexts();
        if (texts.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'texts' or 'text' is missing");
        }

        String modelName = params.modelName();

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
}
