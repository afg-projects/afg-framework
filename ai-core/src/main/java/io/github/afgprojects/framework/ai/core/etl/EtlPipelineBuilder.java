package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * ETL Pipeline 构建器。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class EtlPipelineBuilder {

    private DocumentReader reader;
    private final List<DocumentTransformer> transformers = new ArrayList<>();
    private DocumentWriter writer;
    private ErrorHandler errorHandler;

    /**
     * 设置 Reader。
     */
    @NonNull
    public EtlPipelineBuilder reader(@NonNull DocumentReader reader) {
        this.reader = reader;
        return this;
    }

    /**
     * 添加 Transformer。
     */
    @NonNull
    public EtlPipelineBuilder transformer(@NonNull DocumentTransformer transformer) {
        this.transformers.add(transformer);
        return this;
    }

    /**
     * 添加多个 Transformer。
     */
    @NonNull
    public EtlPipelineBuilder transformers(@NonNull List<DocumentTransformer> transformers) {
        this.transformers.addAll(transformers);
        return this;
    }

    /**
     * 设置 Writer。
     */
    @NonNull
    public EtlPipelineBuilder writer(@NonNull DocumentWriter writer) {
        this.writer = writer;
        return this;
    }

    /**
     * 设置错误处理器。
     */
    @NonNull
    public EtlPipelineBuilder errorHandler(@NonNull ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * 设置错误处理策略（使用默认处理器）。
     */
    @NonNull
    public EtlPipelineBuilder errorHandlingStrategy(@NonNull ErrorHandlingStrategy strategy) {
        this.errorHandler = new DefaultErrorHandler(strategy);
        return this;
    }

    /**
     * 构建 Pipeline。
     */
    @NonNull
    public EtlPipeline build() {
        Objects.requireNonNull(reader, "reader is required");
        Objects.requireNonNull(writer, "writer is required");

        if (errorHandler == null) {
            errorHandler = new DefaultErrorHandler(ErrorHandlingStrategy.FAIL_FAST);
        }

        // 按 order 排序 Transformer
        List<DocumentTransformer> sortedTransformers = transformers.stream()
            .sorted(Comparator.comparingInt(DocumentTransformer::getOrder))
            .toList();

        return new DefaultEtlPipeline(reader, sortedTransformers, writer, errorHandler);
    }
}