package io.github.afgprojects.framework.ai.core.api.etl;

import io.github.afgprojects.framework.ai.core.api.rag.Document;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 默认 ETL Pipeline 实现。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class DefaultEtlPipeline implements EtlPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultEtlPipeline.class);

    private static final int DEFAULT_MAX_ATTEMPTS = 4;

    private final Function<List<Source>, List<Document>> reader;
    private final List<Function<List<Document>, List<Document>>> transformers;
    private final Consumer<List<Document>> writer;
    private final @Nullable Consumer<Exception> errorHandler;

    public DefaultEtlPipeline(
        @NonNull Function<List<Source>, List<Document>> reader,
        @NonNull List<Function<List<Document>, List<Document>>> transformers,
        @NonNull Consumer<List<Document>> writer,
        @Nullable Consumer<Exception> errorHandler
    ) {
        this.reader = reader;
        this.transformers = transformers;
        this.writer = writer;
        this.errorHandler = errorHandler;
    }

    @Override
    public @NonNull EtlResult execute(@NonNull Source source) {
        return executeAll(List.of(source));
    }

    @Override
    public @NonNull EtlResult executeAll(@NonNull List<Source> sources) {
        Instant start = Instant.now();
        EtlContext context = new EtlContext();

        // 1. Read
        List<Document> documents = new ArrayList<>();
        try {
            documents.addAll(reader.apply(sources));
            log.debug("Read {} documents from {} sources", documents.size(), sources.size());
        } catch (Exception e) {
            log.error("Failed to read documents", e);
            context.recordFailure(null, "read", e);
            handleError(e);
            return buildResult(documents, context, start);
        }

        // 2. Transform（带重试支持）
        List<Document> transformed = documents;
        for (int i = 0; i < transformers.size(); i++) {
            Function<List<Document>, List<Document>> transformer = transformers.get(i);
            transformed = executeTransformerWithRetry(transformer, i, transformed, context);
            if (transformed.isEmpty() && !context.getFailures().isEmpty()) {
                // 如果全部失败且使用 FAIL_FAST 策略，提前返回
                break;
            }
        }

        // 3. Write（带重试支持）
        executeWriteWithRetry(transformed, context);

        return buildResult(transformed, context, start);
    }

    /**
     * 执行转换器，支持重试。
     */
    private List<Document> executeTransformerWithRetry(
            Function<List<Document>, List<Document>> transformer,
            int index,
            List<Document> documents,
            EtlContext context) {

        int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        List<Document> result = documents;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                result = transformer.apply(result);
                log.debug("Transformer {} produced {} documents (attempt {}/{})",
                    index, result.size(), attempt, maxAttempts);
                return result;
            } catch (Exception e) {
                log.warn("Transformer {} failed (attempt {}/{}): {}",
                    index, attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    // 等待后重试
                    sleepBeforeRetry();
                    result = documents; // 重置输入
                    continue;
                }

                // 记录失败
                log.error("Transformer {} failed after {} attempts", index, attempt);
                for (Document doc : documents) {
                    context.recordFailure(doc, "transform", e);
                }
                handleError(e);
                return result;
            }
        }

        return result;
    }

    /**
     * 执行写入，支持重试。
     */
    private void executeWriteWithRetry(List<Document> documents, EtlContext context) {
        int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                writer.accept(documents);
                log.debug("Wrote {} documents (attempt {}/{})",
                    documents.size(), attempt, maxAttempts);
                return;
            } catch (Exception e) {
                log.warn("Write failed (attempt {}/{}): {}",
                    attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts) {
                    sleepBeforeRetry();
                    continue;
                }

                // 记录失败
                log.error("Write failed after {} attempts", attempt);
                for (Document doc : documents) {
                    context.recordFailure(doc, "write", e);
                }
                handleError(e);
                return;
            }
        }
    }

    /**
     * 处理错误。
     */
    private void handleError(Exception e) {
        if (errorHandler != null) {
            errorHandler.accept(e);
        }
    }

    /**
     * 重试前等待。
     */
    private void sleepBeforeRetry() {
        try {
            Thread.sleep(Duration.ofSeconds(1).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private EtlResult buildResult(List<Document> documents, EtlContext context, Instant start) {
        List<DocumentFailure> failures = context.getFailures();
        int successCount = documents.size() - failures.size();

        return EtlResult.builder()
            .totalDocuments(documents.size())
            .successCount(Math.max(0, successCount))
            .failureCount(failures.size())
            .successfulDocuments(documents)
            .failures(failures)
            .duration(Duration.between(start, Instant.now()))
            .build();
    }
}
