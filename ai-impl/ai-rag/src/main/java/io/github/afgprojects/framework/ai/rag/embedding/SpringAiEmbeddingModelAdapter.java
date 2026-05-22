package io.github.afgprojects.framework.ai.rag.embedding;

import io.github.afgprojects.framework.ai.core.rag.EmbeddingModel;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring AI EmbeddingModel 适配器
 *
 * <p>将 Spring AI 的 {@link org.springframework.ai.embedding.EmbeddingModel}
 * 适配为框架的 {@link EmbeddingModel} 接口。
 *
 * <p>支持所有 Spring AI 兼容的嵌入模型实现，包括：
 * <ul>
 *   <li>Ollama (spring-ai-ollama)</li>
 *   <li>OpenAI (spring-ai-openai)</li>
 *   <li>Anthropic (spring-ai-anthropic)</li>
 *   <li>其他 Spring AI 支持的模型</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用 Spring AI Ollama
 * OllamaEmbeddingModel springAiModel = new OllamaEmbeddingModel(ollamaApi);
 * EmbeddingModel adapter = new SpringAiEmbeddingModelAdapter(springAiModel, 768);
 *
 * // 生成嵌入向量
 * List<Double> embedding = adapter.embed("Hello, world!");
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiEmbeddingModelAdapter implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(SpringAiEmbeddingModelAdapter.class);

    private final org.springframework.ai.embedding.EmbeddingModel delegate;
    private final int dimension;
    private final int maxBatchSize;

    /**
     * 创建 Spring AI EmbeddingModel 适配器
     *
     * @param delegate  Spring AI 的 EmbeddingModel 实现
     * @param dimension 向量维度
     */
    public SpringAiEmbeddingModelAdapter(org.springframework.ai.embedding.EmbeddingModel delegate,
                                         int dimension) {
        this(delegate, dimension, 100);
    }

    /**
     * 创建 Spring AI EmbeddingModel 适配器（完整参数）
     *
     * @param delegate     Spring AI 的 EmbeddingModel 实现
     * @param dimension    向量维度
     * @param maxBatchSize 最大批量大小
     */
    public SpringAiEmbeddingModelAdapter(org.springframework.ai.embedding.EmbeddingModel delegate,
                                         int dimension, int maxBatchSize) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        this.delegate = delegate;
        this.dimension = dimension;
        this.maxBatchSize = maxBatchSize;

        log.debug("SpringAiEmbeddingModelAdapter initialized: dimension={}, maxBatchSize={}",
            dimension, maxBatchSize);
    }

    @Override
    public @NonNull List<Double> embed(@NonNull String text) {
        log.debug("Embedding text of length: {}", text.length());

        try {
            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = delegate.call(request);

            if (response != null && !response.getResults().isEmpty()) {
                float[] embeddingData = response.getResults().get(0).getOutput();
                List<Double> embedding = new ArrayList<>(embeddingData.length);
                for (float v : embeddingData) {
                    embedding.add((double) v);
                }
                log.debug("Generated embedding of size: {}", embedding.size());
                return embedding;
            }

            throw new RuntimeException("Failed to get embedding from Spring AI: empty response");
        } catch (Exception e) {
            log.error("Failed to get embedding from Spring AI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get embedding from Spring AI: " + e.getMessage(), e);
        }
    }

    @Override
    public @NonNull List<List<Double>> embedBatch(@NonNull List<String> texts) {
        log.debug("Embedding batch of {} texts", texts.size());

        try {
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = delegate.call(request);

            if (response != null && !response.getResults().isEmpty()) {
                List<List<Double>> embeddings = new ArrayList<>(response.getResults().size());
                for (var result : response.getResults()) {
                    float[] embeddingData = result.getOutput();
                    List<Double> embedding = new ArrayList<>(embeddingData.length);
                    for (float v : embeddingData) {
                        embedding.add((double) v);
                    }
                    embeddings.add(embedding);
                }
                log.debug("Generated {} embeddings", embeddings.size());
                return embeddings;
            }

            throw new RuntimeException("Failed to get embeddings from Spring AI: empty response");
        } catch (Exception e) {
            log.error("Failed to get embeddings from Spring AI: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get embeddings from Spring AI: " + e.getMessage(), e);
        }
    }

    @Override
    public int dimension() {
        return dimension;
    }

    @Override
    public @NonNull String modelName() {
        // Spring AI EmbeddingModel 没有统一的 getModelName 方法
        // 返回类名作为默认实现
        return delegate.getClass().getSimpleName();
    }

    @Override
    public int maxBatchSize() {
        return maxBatchSize;
    }
}
