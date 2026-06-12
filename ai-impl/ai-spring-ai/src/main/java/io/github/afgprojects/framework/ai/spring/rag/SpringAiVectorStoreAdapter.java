package io.github.afgprojects.framework.ai.spring.rag;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring AI VectorStore 适配器 - 将 AFG VectorStore + EmbeddingService 桥接为
 * Spring AI 的 {@link org.springframework.ai.vectorstore.VectorStore} 接口实现。
 *
 * <p>核心职责：
 * <ul>
 *   <li>{@code add(List<SpringAiDoc>)} - 调用 AFG EmbeddingService 生成嵌入，写入 AFG VectorStore</li>
 *   <li>{@code delete(List<String>)} - 委托 AFG VectorStore 删除</li>
 *   <li>{@code delete(Filter.Expression)} - 暂不支持，抛出 UnsupportedOperationException</li>
 *   <li>{@code similaritySearch(SearchRequest)} - 委托 AFG VectorStore 搜索，转换结果</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SpringAiVectorStoreAdapter implements org.springframework.ai.vectorstore.VectorStore {

    private final VectorStore afgVectorStore;
    private final EmbeddingService afgEmbeddingService;

    @Override
    public void add(List<org.springframework.ai.document.Document> documents) {
        log.debug("Adding {} documents to AFG VectorStore via Spring AI adapter", documents.size());
        for (org.springframework.ai.document.Document springDoc : documents) {
            Document afgDoc = convertToAfgDocument(springDoc);

            // 如果文档没有嵌入，使用 AFG EmbeddingService 生成
            Document docToAdd = afgDoc;
            if (!afgDoc.hasEmbedding()) {
                List<Double> embedding = afgEmbeddingService.embed(afgDoc.content());
                docToAdd = afgDoc.withEmbedding(embedding);
            }

            afgVectorStore.add(docToAdd);
        }
    }

    @Override
    public void delete(List<String> idList) {
        log.debug("Deleting {} documents from AFG VectorStore via Spring AI adapter", idList.size());
        afgVectorStore.deleteBatch(idList);
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException(
                "Filter-based delete is not supported by SpringAiVectorStoreAdapter. " +
                "Use delete(List<String>) with document IDs instead.");
    }

    @Override
    public List<org.springframework.ai.document.Document> similaritySearch(SearchRequest request) {
        log.debug("Searching AFG VectorStore: query='{}', topK={}, threshold={}",
                request.getQuery(), request.getTopK(), request.getSimilarityThreshold());

        Map<String, Object> filter = null;
        if (request.hasFilterExpression()) {
            filter = convertFilterExpression(request.getFilterExpression());
        }

        List<Document> results;
        if (filter != null) {
            results = afgVectorStore.search(
                    request.getQuery(),
                    request.getTopK(),
                    request.getSimilarityThreshold(),
                    filter
            );
        } else {
            results = afgVectorStore.search(
                    request.getQuery(),
                    request.getTopK(),
                    request.getSimilarityThreshold()
            );
        }

        return results.stream()
                .map(this::convertToSpringDocument)
                .toList();
    }

    // ---- 文档转换 ----

    /**
     * 将 Spring AI Document 转换为 AFG Document。
     * <p>
     * Spring AI Document 包含 id、text、metadata；
     * AFG Document 包含 id、content、embedding、metadata。
     */
    private Document convertToAfgDocument(org.springframework.ai.document.Document springDoc) {
        return Document.of(
                springDoc.getId(),
                springDoc.getText() != null ? springDoc.getText() : "",
                null,
                new HashMap<>(springDoc.getMetadata())
        );
    }

    /**
     * 将 AFG Document 转换为 Spring AI Document。
     * <p>
     * AFG Document 没有独立的 score 字段，相似度分数由搜索时计算。
     * 如果 AFG Document 的 metadata 中包含 "score" 键，则提取为 Spring AI Document 的 score。
     */
    private org.springframework.ai.document.Document convertToSpringDocument(Document afgDoc) {
        var builder = org.springframework.ai.document.Document.builder()
                .id(afgDoc.id())
                .text(afgDoc.content());

        // 提取 score（如果存在于 metadata 中）
        Object scoreValue = afgDoc.metadata().get("score");
        if (scoreValue instanceof Number number) {
            builder.score(number.doubleValue());
        }

        // 复制 metadata（排除 score，避免重复）
        Map<String, Object> metadata = new HashMap<>(afgDoc.metadata());
        metadata.remove("score");
        builder.metadata(metadata);

        return builder.build();
    }

    /**
     * 将 Spring AI Filter.Expression 转换为 AFG 的 Map 过滤器。
     * <p>
     * 目前提供基础支持：对简单的 EQ 表达式（key == value）进行转换。
     * 复杂过滤表达式不支持，返回 null。
     */
    private Map<String, Object> convertFilterExpression(Filter.Expression expression) {
        if (expression == null) {
            return null;
        }

        // 仅支持简单的 EQ 表达式: key == value
        if (expression.type() == Filter.ExpressionType.EQ
                && expression.left() instanceof Filter.Key key
                && expression.right() instanceof Filter.Value value) {
            Map<String, Object> filter = new HashMap<>();
            filter.put(key.key(), value.value());
            return filter;
        }

        log.debug("Unsupported filter expression type: {}, falling back to no filter", expression.type());
        return null;
    }
}
