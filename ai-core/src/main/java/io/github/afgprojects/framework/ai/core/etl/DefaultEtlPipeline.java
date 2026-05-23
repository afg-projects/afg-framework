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

        // 2. Transform
        List<Document> transformed = documents;
        for (DocumentTransformer transformer : transformers) {
            try {
                transformed = transformer.transform(transformed);
                log.debug("Transformer {} produced {} documents", transformer.getName(), transformed.size());
            } catch (Exception e) {
                log.error("Transformer {} failed", transformer.getName(), e);
                for (Document doc : transformed) {
                    if (!errorHandler.handle(doc, e, context)) {
                        return buildResult(transformed, context, start);
                    }
                }
            }
        }

        // 3. Write
        try {
            writer.write(transformed);
            log.debug("Wrote {} documents", transformed.size());
        } catch (Exception e) {
            log.error("Failed to write documents", e);
            for (Document doc : transformed) {
                if (!errorHandler.handle(doc, e, context)) {
                    break;
                }
            }
        }

        return buildResult(transformed, context, start);
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