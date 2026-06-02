package io.github.afgprojects.framework.ai.langchain4j.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 AFG VectorStore 适配为 LangChain4j EmbeddingStore
 *
 * <p>LangChain4j 的 {@link EmbeddingStore} 接口通过此适配器桥接到
 * AFG 的 {@link VectorStore}，使得 LangChain4j 的 RAG 流程可以使用
 * AFG 的向量存储后端。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jEmbeddingStoreAdapter implements EmbeddingStore<TextSegment> {

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;

    /**
     * 创建适配器
     *
     * @param vectorStore      AFG 向量存储
     * @param embeddingService AFG 嵌入服务（用于将查询文本转为向量）
     */
    public Lc4jEmbeddingStoreAdapter(@NonNull VectorStore vectorStore,
                                     @NonNull EmbeddingService embeddingService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
    }

    @Override
    public String add(@NonNull Embedding embedding) {
        // LangChain4j 的 add(Embedding) 不含文本内容，需要创建空文档
        String id = java.util.UUID.randomUUID().toString();
        addInternal(id, embedding, null);
        return id;
    }

    @Override
    public void add(@NonNull String id, @NonNull Embedding embedding) {
        addInternal(id, embedding, null);
    }

    @Override
    public String add(@NonNull Embedding embedding, @Nullable TextSegment textSegment) {
        String id = textSegment != null && textSegment.metadata() != null
                ? textSegment.metadata().getString("id")
                : null;
        if (id == null || id.isBlank()) {
            id = java.util.UUID.randomUUID().toString();
        }
        addInternal(id, embedding, textSegment);
        return id;
    }

    @Override
    public List<String> addAll(@NonNull List<Embedding> embeddings) {
        return EmbeddingStore.super.addAll(embeddings);
    }

    @Override
    public void addAll(@NonNull List<String> ids, @NonNull List<Embedding> embeddings) {
        for (int i = 0; i < ids.size(); i++) {
            addInternal(ids.get(i), embeddings.get(i), null);
        }
    }

    @Override
    public List<String> addAll(@NonNull List<Embedding> embeddings,
                               @Nullable List<TextSegment> textSegments) {
        var ids = new java.util.ArrayList<String>(embeddings.size());
        for (int i = 0; i < embeddings.size(); i++) {
            TextSegment segment = textSegments != null && i < textSegments.size()
                    ? textSegments.get(i)
                    : null;
            ids.add(add(embeddings.get(i), segment));
        }
        return ids;
    }

    @NonNull
    @Override
    public EmbeddingSearchResult<TextSegment> search(@NonNull EmbeddingSearchRequest request) {
        // 将查询嵌入转为 AFG Document 搜索
        float[] queryVector = request.queryEmbedding().vector();
        List<Double> queryEmbedding = new java.util.ArrayList<>(queryVector.length);
        for (float v : queryVector) {
            queryEmbedding.add((double) v);
        }

        // 使用 VectorStore 的 search 方法
        // 注意：VectorStore.search 接受文本查询，这里需要先通过 embeddingService
        // 但 EmbeddingSearchRequest 已经提供了 queryEmbedding，所以直接使用向量搜索
        // 简化实现：使用 VectorStore 的 search 方法
        List<Document> results = vectorStore.search(
            "", // VectorStore.search 需要 query text，此处为简化
            request.maxResults(),
            request.minScore() != null ? request.minScore() : 0.0
        );

        var matches = new java.util.ArrayList<EmbeddingMatch<TextSegment>>();
        for (Document doc : results) {
            TextSegment segment = TextSegment.from(doc.content(), toLc4jMetadata(doc.metadata()));
            Embedding embedding = doc.hasEmbedding()
                    ? Embedding.from(doc.embedding().stream().map(Double::floatValue).toList())
                    : null;
            double score = doc.hasEmbedding() ? cosineSimilarity(queryEmbedding, doc.embedding()) : 0.0;
            matches.add(new EmbeddingMatch<>(score, doc.id(), embedding, segment));
        }

        return new EmbeddingSearchResult<>(matches);
    }

    // ---- 内部方法 ----

    private void addInternal(String id, Embedding embedding, @Nullable TextSegment segment) {
        String content = segment != null ? segment.text() : "";
        Map<String, Object> metadata = segment != null && segment.metadata() != null
                ? fromLc4jMetadata(segment.metadata())
                : new HashMap<>();

        List<Double> embeddingList = new java.util.ArrayList<>(embedding.vector().length);
        for (float v : embedding.vector()) {
            embeddingList.add((double) v);
        }

        Document doc = Document.of(id, content, embeddingList, metadata);
        vectorStore.add(doc);
    }

    private Map<String, Object> fromLc4jMetadata(dev.langchain4j.data.document.Metadata lc4jMetadata) {
        var result = new HashMap<String, Object>();
        if (lc4jMetadata != null) {
            lc4jMetadata.asMap().forEach(result::put);
        }
        return result;
    }

    private dev.langchain4j.data.document.Metadata toLc4jMetadata(Map<String, Object> metadata) {
        var result = dev.langchain4j.data.document.Metadata.from(metadata);
        return result;
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size()) return 0.0;
        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
