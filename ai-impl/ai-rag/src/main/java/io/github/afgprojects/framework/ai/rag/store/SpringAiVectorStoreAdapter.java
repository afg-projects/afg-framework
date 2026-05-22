package io.github.afgprojects.framework.ai.rag.store;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.FilterExpression;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI VectorStore 适配器
 *
 * <p>将 Spring AI 的 VectorStore 实现适配为框架的 VectorStore 接口。
 * 支持所有 Spring AI 提供的向量存储实现，如：
 * <ul>
 *   <li>PgVectorStore - PostgreSQL pgvector</li>
 *   <li>ChromaVectorStore - Chroma</li>
 *   <li>PineconeVectorStore - Pinecone</li>
 *   <li>MilvusVectorStore - Milvus</li>
 *   <li>WeaviateVectorStore - Weaviate</li>
 *   <li>RedisVectorStore - Redis</li>
 *   <li>SimpleVectorStore - 内存存储</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 使用 Spring AI 的 PgVectorStore
 * PgVectorStore pgVectorStore = new PgVectorStore(jdbcTemplate, embeddingModel);
 * VectorStore adapter = new SpringAiVectorStoreAdapter(pgVectorStore);
 *
 * // 添加文档
 * adapter.add(documents);
 *
 * // 相似度搜索
 * List<Document> results = adapter.similaritySearch("query", 5);
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiVectorStoreAdapter implements io.github.afgprojects.framework.ai.core.rag.VectorStore {

    private final org.springframework.ai.vectorstore.VectorStore delegate;

    /**
     * 创建适配器实例
     *
     * @param delegate Spring AI 的 VectorStore 实现
     * @throws IllegalArgumentException 如果 delegate 为 null
     */
    public SpringAiVectorStoreAdapter(org.springframework.ai.vectorstore.VectorStore delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate cannot be null");
        }
        this.delegate = delegate;
    }

    @Override
    public void add(@NonNull List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("documents cannot be null or empty");
        }

        List<org.springframework.ai.document.Document> springAiDocs = documents.stream()
                .map(this::convertToSpringAiDocument)
                .collect(Collectors.toList());

        delegate.add(springAiDocs);
    }

    @Override
    public void delete(@NonNull List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids cannot be null or empty");
        }

        delegate.delete(ids);
    }

    @Override
    public void update(@NonNull Document document) {
        if (document == null) {
            throw new IllegalArgumentException("document cannot be null");
        }

        // Spring AI 的 VectorStore 没有直接的 update 方法
        // 先删除再添加
        delete(document.id());
        add(List.of(document));
    }

    @Override
    public @Nullable Document getById(@NonNull String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }

        // Spring AI 的 VectorStore 没有直接的 getById 方法
        // 使用相似度搜索来模拟，搜索 id 本身
        // 这是一个限制，实际使用时可能需要其他方式
        List<org.springframework.ai.document.Document> results = delegate.similaritySearch(id);

        if (results.isEmpty()) {
            return null;
        }

        // 检查是否匹配
        org.springframework.ai.document.Document doc = results.getFirst();
        if (id.equals(doc.getId())) {
            return convertFromSpringAiDocument(doc);
        }

        return null;
    }

    @Override
    public boolean exists(@NonNull String id) {
        return getById(id) != null;
    }

    @Override
    public @NonNull List<Document> similaritySearch(@NonNull String query, int k) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or blank");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k cannot be negative");
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .build();

        List<org.springframework.ai.document.Document> results = delegate.similaritySearch(request);

        return results.stream()
                .map(this::convertFromSpringAiDocument)
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull List<Document> similaritySearch(
            @NonNull String query,
            int k,
            @Nullable FilterExpression filter
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query cannot be null or blank");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k cannot be negative");
        }

        if (filter == null) {
            return similaritySearch(query, k);
        }

        // 转换过滤器
        Filter.Expression springAiFilter = convertFilter(filter);

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(k)
                .filterExpression(springAiFilter)
                .build();

        List<org.springframework.ai.document.Document> results = delegate.similaritySearch(request);

        return results.stream()
                .map(this::convertFromSpringAiDocument)
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull List<Document> similaritySearchByEmbedding(@NonNull List<Double> embedding, int k) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("embedding cannot be null or empty");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k cannot be negative");
        }

        // Spring AI 的 VectorStore 使用 SearchRequest 进行搜索
        // 需要通过 embedding 进行搜索，但 SearchRequest 主要基于 query text
        // 这是一个限制，需要具体实现来支持 embedding 搜索
        throw new UnsupportedOperationException(
                "similaritySearchByEmbedding is not directly supported by Spring AI VectorStore. " +
                "Use similaritySearch with query text instead, or use the underlying implementation directly."
        );
    }

    @Override
    public @NonNull List<Document> similaritySearchByEmbedding(
            @NonNull List<Double> embedding,
            int k,
            @Nullable FilterExpression filter
    ) {
        if (embedding == null || embedding.isEmpty()) {
            throw new IllegalArgumentException("embedding cannot be null or empty");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k cannot be negative");
        }

        // Spring AI 的 VectorStore 使用 SearchRequest 进行搜索
        throw new UnsupportedOperationException(
                "similaritySearchByEmbedding is not directly supported by Spring AI VectorStore. " +
                "Use similaritySearch with query text instead, or use the underlying implementation directly."
        );
    }

    @Override
    public long count() {
        // Spring AI 的 VectorStore 没有直接的 count 方法
        // 这是一个限制，需要具体实现来支持
        throw new UnsupportedOperationException(
                "count() is not supported by Spring AI VectorStore. " +
                "Please use the underlying implementation directly."
        );
    }

    @Override
    public void clear() {
        // Spring AI 的 VectorStore 没有直接的 clear 方法
        // 这是一个限制，需要具体实现来支持
        throw new UnsupportedOperationException(
                "clear() is not supported by Spring AI VectorStore. " +
                "Please use the underlying implementation directly."
        );
    }

    /**
     * 将框架的 Document 转换为 Spring AI 的 Document
     *
     * @param doc 框架的 Document
     * @return Spring AI 的 Document
     */
    private org.springframework.ai.document.Document convertToSpringAiDocument(@NonNull Document doc) {
        Map<String, Object> metadata = new HashMap<>(doc.metadata());

        // 创建 Spring AI Document
        return new org.springframework.ai.document.Document(
                doc.id(),
                doc.content(),
                metadata
        );
    }

    /**
     * 将 Spring AI 的 Document 转换为框架的 Document
     *
     * @param doc Spring AI 的 Document
     * @return 框架的 Document
     */
    private Document convertFromSpringAiDocument(org.springframework.ai.document.Document doc) {
        // Spring AI Document 使用 getText() 获取文本内容
        String content = doc.getText();
        if (content == null) {
            content = ""; // 默认空内容
        }

        return new Document(
                doc.getId(),
                content,
                null, // Spring AI Document 没有 embedding getter
                new HashMap<>(doc.getMetadata())
        );
    }

    /**
     * 将框架的 FilterExpression 转换为 Spring AI 的 Filter.Expression
     *
     * @param filter 框架的 FilterExpression
     * @return Spring AI 的 Filter.Expression
     */
    private Filter.Expression convertFilter(@NonNull FilterExpression filter) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return convertFilterExpression(filter, builder);
    }

    /**
     * 递归转换 FilterExpression
     */
    private Filter.Expression convertFilterExpression(FilterExpression expr, FilterExpressionBuilder builder) {
        if (expr instanceof FilterExpression.Comparison comparison) {
            return convertComparison(comparison, builder);
        } else if (expr instanceof FilterExpression.InFilter inFilter) {
            return convertInFilter(inFilter, builder);
        } else if (expr instanceof FilterExpression.NotInFilter notInFilter) {
            return convertNotInFilter(notInFilter, builder);
        } else if (expr instanceof FilterExpression.AndFilter andFilter) {
            return convertAndFilter(andFilter, builder);
        } else if (expr instanceof FilterExpression.OrFilter orFilter) {
            return convertOrFilter(orFilter, builder);
        } else if (expr instanceof FilterExpression.NotFilter notFilter) {
            return convertNotFilter(notFilter, builder);
        } else {
            throw new IllegalArgumentException("Unknown filter expression type: " + expr.getClass());
        }
    }

    private Filter.Expression convertComparison(FilterExpression.Comparison comparison, FilterExpressionBuilder builder) {
        String field = comparison.field();
        Object value = comparison.value();

        return switch (comparison.operator()) {
            case EQ -> builder.eq(field, value).build();
            case NE -> builder.ne(field, value).build();
            case GT -> builder.gt(field, value).build();
            case GTE -> builder.gte(field, value).build();
            case LT -> builder.lt(field, value).build();
            case LTE -> builder.lte(field, value).build();
            default -> throw new IllegalArgumentException(
                    "Unsupported comparison operator: " + comparison.operator()
            );
        };
    }

    private Filter.Expression convertInFilter(FilterExpression.InFilter inFilter, FilterExpressionBuilder builder) {
        return builder.in(inFilter.field(), inFilter.values().toArray()).build();
    }

    private Filter.Expression convertNotInFilter(FilterExpression.NotInFilter notInFilter, FilterExpressionBuilder builder) {
        return builder.nin(notInFilter.field(), notInFilter.values().toArray()).build();
    }

    private Filter.Expression convertAndFilter(FilterExpression.AndFilter andFilter, FilterExpressionBuilder builder) {
        FilterExpressionBuilder.Op result = null;

        for (FilterExpression expr : andFilter.filters()) {
            FilterExpressionBuilder.Op current = new FilterExpressionBuilder.Op(convertFilterExpression(expr, builder));
            if (result == null) {
                result = current;
            } else {
                result = builder.and(result, current);
            }
        }

        return result != null ? result.build() : null;
    }

    private Filter.Expression convertOrFilter(FilterExpression.OrFilter orFilter, FilterExpressionBuilder builder) {
        FilterExpressionBuilder.Op result = null;

        for (FilterExpression expr : orFilter.filters()) {
            FilterExpressionBuilder.Op current = new FilterExpressionBuilder.Op(convertFilterExpression(expr, builder));
            if (result == null) {
                result = current;
            } else {
                result = builder.or(result, current);
            }
        }

        return result != null ? result.build() : null;
    }

    private Filter.Expression convertNotFilter(FilterExpression.NotFilter notFilter, FilterExpressionBuilder builder) {
        Filter.Expression innerExpr = convertFilterExpression(notFilter.filter(), builder);
        return builder.not(new FilterExpressionBuilder.Op(innerExpr)).build();
    }
}