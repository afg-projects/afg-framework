package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.etl.cleaner.ContentCleaner;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容清洗转换器。
 *
 * <p>使用 ContentCleaner 列表清洗文档内容。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class ContentCleanerTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(ContentCleanerTransformer.class);

    private final List<ContentCleaner> cleaners;

    /**
     * 创建内容清洗转换器。
     *
     * @param cleaners 内容清洗器列表
     */
    public ContentCleanerTransformer(@NonNull List<ContentCleaner> cleaners) {
        this.cleaners = new ArrayList<>(cleaners);
    }

    /**
     * 创建默认配置的内容清洗转换器。
     */
    public ContentCleanerTransformer() {
        this.cleaners = new ArrayList<>();
    }

    /**
     * 添加内容清洗器。
     *
     * @param cleaner 内容清洗器
     * @return this
     */
    public ContentCleanerTransformer addCleaner(@NonNull ContentCleaner cleaner) {
        this.cleaners.add(cleaner);
        return this;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        if (cleaners.isEmpty()) {
            log.debug("No cleaners configured, returning documents unchanged");
            return documents;
        }

        List<Document> result = new ArrayList<>(documents.size());

        for (Document doc : documents) {
            String content = doc.content();

            for (ContentCleaner cleaner : cleaners) {
                try {
                    content = cleaner.clean(content);
                } catch (Exception e) {
                    log.warn("Cleaner {} failed for document {}: {}",
                        cleaner.getName(), doc.id(), e.getMessage());
                }
            }

            // 只有内容改变时才创建新文档
            if (content.equals(doc.content())) {
                result.add(doc);
            } else {
                result.add(new Document(
                    doc.id(),
                    content,
                    doc.embedding(),
                    doc.metadata()
                ));
            }
        }

        log.debug("Cleaned {} documents with {} cleaners", documents.size(), cleaners.size());
        return result;
    }

    @Override
    public @NonNull String getName() {
        return "ContentCleanerTransformer";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
