package io.github.afgprojects.framework.ai.langchain4j.chat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;

import java.util.List;
import java.util.Objects;

/**
 * AfgEmbeddingClient 的 LangChain4j 实现 - 委托 LangChain4j EmbeddingModel
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jEmbeddingClient implements AfgEmbeddingClient {

    private final EmbeddingModel embeddingModel;
    private final String modelName;

    public Lc4jEmbeddingClient(EmbeddingModel embeddingModel) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        this.modelName = null;
    }

    private Lc4jEmbeddingClient(EmbeddingModel embeddingModel, String modelName) {
        this.embeddingModel = Objects.requireNonNull(embeddingModel, "embeddingModel must not be null");
        this.modelName = modelName;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        Response<List<Embedding>> response = embeddingModel.embedAll(texts.stream()
                .map(dev.langchain4j.data.segment.TextSegment::from)
                .toList());
        return response.content().stream()
                .map(Embedding::vector)
                .toList();
    }

    @Override
    public float[] embed(String text) {
        Response<Embedding> response = embeddingModel.embed(
                dev.langchain4j.data.segment.TextSegment.from(text));
        return response.content().vector();
    }

    @Override
    public AfgEmbeddingClient withModel(String modelName) {
        return new Lc4jEmbeddingClient(this.embeddingModel, modelName);
    }
}
