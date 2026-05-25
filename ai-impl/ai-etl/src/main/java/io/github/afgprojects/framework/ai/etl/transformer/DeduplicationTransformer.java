package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.rag.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档去重转换器。
 *
 * <p>使用 DeduplicationStrategy 移除重复的文档。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class DeduplicationTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationTransformer.class);

    private final DeduplicationStrategy strategy;

    /**
     * 创建去重转换器。
     *
     * @param strategy 去重策略
     */
    public DeduplicationTransformer(@NonNull DeduplicationStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * 创建默认配置的去重转换器（使用内容去重策略）。
     */
    public DeduplicationTransformer() {
        this(DeduplicationStrategy.contentBased());
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        Set<String> seenKeys = new HashSet<>();
        List<Document> result = new ArrayList<>();
        int duplicatesFound = 0;

        for (Document doc : documents) {
            String key = strategy.computeKey(doc);

            if (!seenKeys.contains(key)) {
                seenKeys.add(key);
                result.add(doc);
            } else {
                duplicatesFound++;
                log.debug("Duplicate document found: {} (key: {})", doc.id(), key);
            }
        }

        if (duplicatesFound > 0) {
            log.info("Removed {} duplicate documents using {}", duplicatesFound, strategy.getName());
        }

        log.debug("Deduplication: {} -> {} documents", documents.size(), result.size());
        return result;
    }

    @Override
    public @NonNull String getName() {
        return "DeduplicationTransformer";
    }

    @Override
    public int getOrder() {
        return 20;
    }
}