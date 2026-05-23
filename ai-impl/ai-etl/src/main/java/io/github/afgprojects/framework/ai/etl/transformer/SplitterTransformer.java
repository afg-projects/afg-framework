package io.github.afgprojects.framework.ai.etl.transformer;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.DocumentTransformer;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 文本分割转换器。
 *
 * <p>使用 TextSplitter 将文档分割为多个较小的块，
 * 适用于 RAG 场景中向量嵌入的预处理。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class SplitterTransformer implements DocumentTransformer {

    private static final Logger log = LoggerFactory.getLogger(SplitterTransformer.class);

    private final TextSplitter splitter;

    /**
     * 创建分割转换器。
     *
     * @param splitter 文本分割器
     */
    public SplitterTransformer(@NonNull TextSplitter splitter) {
        this.splitter = splitter;
    }

    @Override
    public @NonNull List<Document> transform(@NonNull List<Document> documents) {
        List<Document> result = documents.stream()
            .flatMap(doc -> splitter.splitDocument(doc).stream())
            .toList();

        log.debug("Split {} documents into {} chunks", documents.size(), result.size());
        return result;
    }

    @Override
    public @NonNull String getName() {
        return "SplitterTransformer";
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
