package io.github.afgprojects.framework.ai.rag;

import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple embedding service that generates random vectors.
 *
 * <p>This is a placeholder implementation for development and testing purposes.
 * It does NOT produce meaningful embeddings and should NOT be used in production.
 * For production use, configure a real embedding service (e.g., OpenAI, Ollama).
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class SimpleEmbeddingService implements EmbeddingService {

    private final int dimensions;
    private final Random random;

    /**
     * Creates a SimpleEmbeddingService with the specified dimensions.
     *
     * @param dimensions the embedding vector dimensionality
     */
    public SimpleEmbeddingService(int dimensions) {
        this.dimensions = dimensions;
        this.random = new Random(42);
        log.warn("SimpleEmbeddingService initialized with {} dimensions. " +
                 "This produces random vectors and should NOT be used in production!", dimensions);
    }

    @Override
    @NonNull
    public List<Double> embed(@NonNull String text) {
        List<Double> embedding = new ArrayList<>(dimensions);
        for (int i = 0; i < dimensions; i++) {
            embedding.add(random.nextDouble());
        }
        return embedding;
    }

    @Override
    @NonNull
    public List<List<Double>> embedBatch(@NonNull List<String> texts) {
        return texts.stream().map(this::embed).toList();
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    @Nullable
    public String getModelId() {
        return "simple-random";
    }
}
