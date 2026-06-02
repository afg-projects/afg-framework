package io.github.afgprojects.framework.ai.core.api.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Service for generating vector embeddings from text.
 *
 * <p>An embedding is a vector representation of text that captures semantic meaning,
 * enabling similarity search in vector stores.
 *
 * <p>Example usage:
 * <pre>{@code
 * EmbeddingService service = ...;
 * List<Double> embedding = service.embed("Hello, world!");
 * List<List<Double>> embeddings = service.embedBatch(List.of("Hello", "World"));
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface EmbeddingService {

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text the text to embed, must not be null
     * @return the embedding vector as a list of doubles
     */
    @NonNull
    List<Double> embed(@NonNull String text);

    /**
     * Generates embedding vectors for multiple texts in batch.
     *
     * <p>Default implementation calls {@link #embed(String)} for each text sequentially.
     * Implementations should override this method for better batch performance.
     *
     * @param texts the texts to embed, must not be null
     * @return the list of embedding vectors, one per text
     */
    @NonNull
    default List<List<Double>> embedBatch(@NonNull List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    /**
     * Returns the dimensionality of the embedding vectors produced by this service.
     *
     * @return the embedding dimension, or -1 if unknown
     */
    int dimensions();

    /**
     * Returns the model identifier used by this embedding service.
     *
     * @return the model identifier, or null if not applicable
     */
    @Nullable
    default String getModelId() {
        return null;
    }
}
