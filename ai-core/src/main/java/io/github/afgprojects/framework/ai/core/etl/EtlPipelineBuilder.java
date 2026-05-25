package io.github.afgprojects.framework.ai.core.etl;

import io.github.afgprojects.framework.ai.core.rag.Document;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ETL Pipeline 构建器。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class EtlPipelineBuilder {

    private Function<List<Source>, List<Document>> reader;
    private final List<Function<List<Document>, List<Document>>> transformers = new ArrayList<>();
    private Consumer<List<Document>> writer;
    private @Nullable Consumer<Exception> errorHandler;

    /**
     * 设置 Reader。
     *
     * @param reader 读取函数，接收数据源列表，返回文档列表
     */
    @NonNull
    public EtlPipelineBuilder reader(@NonNull Function<List<Source>, List<Document>> reader) {
        this.reader = reader;
        return this;
    }

    /**
     * 添加 Transformer。
     *
     * @param transformer 转换函数，接收文档列表，返回转换后的文档列表
     */
    @NonNull
    public EtlPipelineBuilder transformer(@NonNull Function<List<Document>, List<Document>> transformer) {
        this.transformers.add(transformer);
        return this;
    }

    /**
     * 添加多个 Transformer。
     *
     * @param transformers 转换函数列表
     */
    @NonNull
    public EtlPipelineBuilder transformers(@NonNull List<Function<List<Document>, List<Document>>> transformers) {
        this.transformers.addAll(transformers);
        return this;
    }

    /**
     * 设置 Writer。
     *
     * @param writer 写入函数，接收文档列表
     */
    @NonNull
    public EtlPipelineBuilder writer(@NonNull Consumer<List<Document>> writer) {
        this.writer = writer;
        return this;
    }

    /**
     * 设置错误处理器。
     *
     * @param errorHandler 错误处理函数，接收异常
     */
    @NonNull
    public EtlPipelineBuilder errorHandler(@NonNull Consumer<Exception> errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * 设置错误处理策略（使用默认处理器）。
     */
    @NonNull
    public EtlPipelineBuilder errorHandlingStrategy(@NonNull ErrorHandlingStrategy strategy) {
        this.errorHandler = switch (strategy) {
            case FAIL_FAST -> e -> { throw e instanceof RuntimeException re ? re : new RuntimeException(e); };
            case CONTINUE, SKIP_AND_LOG -> e -> {};
            case RETRY -> e -> {};
        };
        return this;
    }

    /**
     * 构建 Pipeline。
     */
    @NonNull
    public EtlPipeline build() {
        Objects.requireNonNull(reader, "reader is required");
        Objects.requireNonNull(writer, "writer is required");

        return new DefaultEtlPipeline(reader, transformers, writer, errorHandler);
    }
}
