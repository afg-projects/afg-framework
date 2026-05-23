package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认 ETL Pipeline 实现。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class DefaultEtlPipeline implements EtlPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultEtlPipeline.class);

    private final DocumentReader reader;
    private final List<DocumentTransformer> transformers;
    private final DocumentWriter writer;
    private final ErrorHandler errorHandler;

    public DefaultEtlPipeline(
        @NonNull DocumentReader reader,
        @NonNull List<DocumentTransformer> transformers,
        @NonNull DocumentWriter writer,
        @NonNull ErrorHandler errorHandler
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
            documents.addAll(reader.readAll(sources));
            log.debug("Read {} documents from {} sources", documents.size(), sources.size());
        } catch (Exception e) {
            log.error("Failed to read documents", e);
            context.recordFailure(null, "read", e);
            return buildResult(documents, context, start);
        }

        // 2. Transform（带重试支持）
        List<Document> transformed = documents;
        for (DocumentTransformer transformer : transformers) {
            transformed = executeTransformerWithRetry(transformer, transformed, context);
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
            DocumentTransformer transformer,
            List<Document> documents,
            EtlContext context) {

        int maxAttempts = getMaxRetryAttempts();
        List<Document> result = documents;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                result = transformer.transform(result);
                log.debug("Transformer {} produced {} documents (attempt {}/{})",
                    transformer.getName(), result.size(), attempt, maxAttempts);
                return result;
            } catch (Exception e) {
                log.warn("Transformer {} failed (attempt {}/{}): {}",
                    transformer.getName(), attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts && shouldRetryOnError(e, context)) {
                    // 等待后重试
                    sleepBeforeRetry();
                    result = documents; // 重置输入
                    continue;
                }

                // 记录失败
                log.error("Transformer {} failed after {} attempts", transformer.getName(), attempt);
                for (Document doc : documents) {
                    if (!errorHandler.handle(doc, e, context)) {
                        return result; // FAIL_FAST，提前返回
                    }
                }
                return result;
            }
        }

        return result;
    }

    /**
     * 执行写入，支持重试。
     */
    private void executeWriteWithRetry(List<Document> documents, EtlContext context) {
        int maxAttempts = getMaxRetryAttempts();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                writer.write(documents);
                log.debug("Wrote {} documents (attempt {}/{})",
                    documents.size(), attempt, maxAttempts);
                return;
            } catch (Exception e) {
                log.warn("Write failed (attempt {}/{}): {}",
                    attempt, maxAttempts, e.getMessage());

                if (attempt < maxAttempts && shouldRetryOnError(e, context)) {
                    sleepBeforeRetry();
                    continue;
                }

                // 记录失败
                log.error("Write failed after {} attempts", attempt);
                for (Document doc : documents) {
                    if (!errorHandler.handle(doc, e, context)) {
                        break;
                    }
                }
                return;
            }
        }
    }

    /**
     * 获取最大重试次数。
     */
    private int getMaxRetryAttempts() {
        if (errorHandler instanceof DefaultErrorHandler defaultHandler) {
            return defaultHandler.getMaxRetries() + 1; // +1 表示首次尝试
        }
        return 4; // 默认 3 次重试 + 1 次首次尝试
    }

    /**
     * 判断是否应该重试。
     */
    private boolean shouldRetryOnError(Exception error, EtlContext context) {
        return errorHandler instanceof DefaultErrorHandler;
    }

    /**
     * 重试前等待。
     */
    private void sleepBeforeRetry() {
        Duration delay = Duration.ofSeconds(1);
        if (errorHandler instanceof DefaultErrorHandler defaultHandler) {
            delay = defaultHandler.getRetryDelay();
        }
        try {
            Thread.sleep(delay.toMillis());
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