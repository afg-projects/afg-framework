package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据丰富转换器。
 *
 * <p>使用 MetadataExtractor 列表为文档添加额外的元数据。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class MetadataEnricherTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(MetadataEnricherTransformer.class);

    private final List<MetadataExtractor> extractors;

    /**
     * 创建元数据丰富转换器。
     *
     * @param extractors 元数据提取器列表
     */
    public MetadataEnricherTransformer(@NonNull List<MetadataExtractor> extractors) {
        this.extractors = new ArrayList<>(extractors);
    }

    /**
     * 创建默认配置的元数据丰富转换器。
     */
    public MetadataEnricherTransformer() {
        this.extractors = new ArrayList<>();
    }

    /**
     * 添加元数据提取器。
     *
     * @param extractor 元数据提取器
     * @return this
     */
    public MetadataEnricherTransformer addExtractor(@NonNull MetadataExtractor extractor) {
        this.extractors.add(extractor);
        return this;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        if (extractors.isEmpty()) {
            log.debug("No extractors configured, returning documents unchanged");
            return documents;
        }

        List<Document> result = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            Map<String, Object> enrichedMetadata = new HashMap<>(doc.metadata());

            for (MetadataExtractor extractor : extractors) {
                try {
                    Map<String, Object> extracted = extractor.extract(doc);
                    enrichedMetadata.putAll(extracted);
                } catch (Exception e) {
                    log.warn("Extractor {} failed for document {}: {}",
                        extractor.getName(), doc.id(), e.getMessage());
                }
            }

            result.add(new Document(
                doc.id(),
                doc.content(),
                doc.embedding(),
                enrichedMetadata
            ));
        }

        log.debug("Enriched {} documents with {} extractors", documents.size(), extractors.size());
        return result;
    }

    @Override
    public @NonNull String getName() {
        return "MetadataEnricherTransformer";
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
