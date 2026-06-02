package io.github.afgprojects.framework.ai.langchain4j.chat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * AfgEmbeddingClient 的 LangChain4j 实现 - 委托 LangChain4j EmbeddingModel
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jEmbeddingClient implements AfgEmbeddingClient {

    private final EmbeddingModel embeddingModel;
    private final String modelName;

    public Lc4jEmbeddingClient(@NonNull EmbeddingModel embeddingModel) {
        this(embeddingModel, null);
    }

    private Lc4jEmbeddingClient(@NonNull EmbeddingModel embeddingModel, @Nullable String modelName) {
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }

    @Override
    @NonNull
    public List<float[]> embed(@NonNull List<String> texts) {
        Response<List<Embedding>> response = embeddingModel.embedAll(texts.stream()
                .map(dev.langchain4j.data.segment.TextSegment::from)
                .toList());
        return response.content().stream()
                .map(Embedding::vector)
                .toList();
    }

    @Override
    @NonNull
    public float[] embed(@NonNull String text) {
        Response<Embedding> response = embeddingModel.embed(
                dev.langchain4j.data.segment.TextSegment.from(text));
        return response.content().vector();
    }

    @Override
    @NonNull
    public AfgEmbeddingClient withModel(@Nullable String modelName) {
        return new Lc4jEmbeddingClient(this.embeddingModel, modelName);
    }
}
