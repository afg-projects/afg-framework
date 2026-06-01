package io.github.afgprojects.framework.ai.chat;

import io.github.afgprojects.framework.ai.core.chat.AfgEmbeddingClient;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

/**
 * AfgEmbeddingClient 默认实现 - 委托 Spring AI EmbeddingModel
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiEmbeddingClient implements AfgEmbeddingClient {

    private final EmbeddingModel embeddingModel;
    private final String modelName;

    public SpringAiEmbeddingClient(@NonNull EmbeddingModel embeddingModel) {
        this(embeddingModel, null);
    }

    private SpringAiEmbeddingClient(@NonNull EmbeddingModel embeddingModel, @Nullable String modelName) {
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }

    @Override
    @NonNull
    public List<float[]> embed(@NonNull List<String> texts) {
        EmbeddingOptions options = modelName != null
                ? EmbeddingOptions.builder().model(modelName).build()
                : null;
        EmbeddingRequest request = new EmbeddingRequest(texts, options);
        EmbeddingResponse response = embeddingModel.call(request);
        return response.getResults().stream()
                .map(result -> result.getOutput())
                .toList();
    }

    @Override
    @NonNull
    public float[] embed(@NonNull String text) {
        EmbeddingOptions options = modelName != null
                ? EmbeddingOptions.builder().model(modelName).build()
                : null;
        EmbeddingRequest request = new EmbeddingRequest(List.of(text), options);
        EmbeddingResponse response = embeddingModel.call(request);
        return response.getResult().getOutput();
    }

    @Override
    @NonNull
    public AfgEmbeddingClient withModel(@Nullable String modelName) {
        return new SpringAiEmbeddingClient(this.embeddingModel, modelName);
    }
}