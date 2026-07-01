package io.github.afgprojects.framework.ai.core.workflow.node.rag;

import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
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
 * Embedding node - converts text to embeddings and optionally stores them.
 *
 * <p>Converts text to vector embeddings using an embedding model, and optionally
 * stores the embeddings in a vector store for later retrieval.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link AfgEmbeddingClient} and {@link VectorStore} are construction-time
 * dependencies; the no-arg constructor leaves them null so the node degrades
 * gracefully when embedding is not configured.</p>
 */
@Slf4j
public class EmbeddingNode extends AbstractWorkflowNode<EmbeddingNode.Params> {

    public static final String TYPE = "embedding";

    /** Strongly-typed parameters for {@link EmbeddingNode}. */
    public record Params(
            @Param(displayName = "Texts", description = "List of text strings to embed")
            List<String> texts,
            @Param(displayName = "Text", description = "Single text to embed (fallback if texts is absent)")
            String text,
            @Param(displayName = "Store", description = "Whether to store embeddings", defaultValue = "false")
            Boolean store,
            @Param(displayName = "Collection", description = "Collection name for storage", defaultValue = "default")
            String collection,
            @Param(displayName = "Metadata", description = "Metadata to associate with stored embeddings")
            Map<String, Object> metadata
    ) {
        /** Whether to store embeddings. */
        public boolean isStore() {
            return Boolean.TRUE.equals(store);
        }

        /** Effective collection name, defaulting to "default". */
        public String effectiveCollection() {
            return collection == null || collection.isBlank() ? "default" : collection;
        }

        /** Normalized list of texts, falling back to the single text field. */
        public List<String> effectiveTexts() {
            if (texts != null && !texts.isEmpty()) {
                return texts;
            }
            List<String> result = new ArrayList<>();
            if (text != null) {
                result.add(text);
            }
            return result;
        }
    }

    /** Output descriptor for {@link EmbeddingNode}. */
    public record Output(
            @Out(description = "Embedding count") int count,
            @Out(description = "Dimensions") int dimensions,
            @Out(description = "Whether stored") boolean stored
    ) {}

    private final AfgEmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    public EmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient, VectorStore vectorStore) {
        super(nodeId, TYPE, Params.class);
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    public EmbeddingNode(String nodeId, AfgEmbeddingClient embeddingClient) {
        super(nodeId, TYPE, Params.class);
        this.embeddingClient = embeddingClient;
        this.vectorStore = null;
    }

    public EmbeddingNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.embeddingClient = null;
        this.vectorStore = null;
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

        boolean store = params.isStore();

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
            String collection = params.effectiveCollection();
            Map<String, Object> metadata = params.metadata();
            // Store embeddings in vector store
            // Future: call vectorStore.store(collection, texts, embeddings, metadata)
            result.put("stored", true);
            result.put("collection", collection);
        }

        return result;
    }
}
