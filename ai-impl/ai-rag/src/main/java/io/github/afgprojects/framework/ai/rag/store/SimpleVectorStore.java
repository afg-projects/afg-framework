package io.github.afgprojects.framework.ai.rag.store;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.VectorStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 简单内存向量存储实现
 *
 * <p>基于内存的向量存储，适用于开发测试和小规模场景。
 * 不支持持久化，应用重启后数据丢失。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SimpleVectorStore implements VectorStore {

    private final Map<String, Document> documents = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> embeddings = new ConcurrentHashMap<>();

    @Override
    public void add(@NonNull List<Document> documentList) {
        for (Document doc : documentList) {
            documents.put(doc.id(), doc);
            if (doc.embedding() != null) {
                embeddings.put(doc.id(), doc.embedding());
            }
        }
    }

    @Override
    public void delete(@NonNull List<String> ids) {
        for (String id : ids) {
            documents.remove(id);
            embeddings.remove(id);
        }
    }

    @Override
    public void update(@NonNull Document document) {
        documents.put(document.id(), document);
        if (document.embedding() != null) {
            embeddings.put(document.id(), document.embedding());
        }
    }

    @Override
    public @Nullable Document getById(@NonNull String id) {
        return documents.get(id);
    }

    @Override
    public boolean exists(@NonNull String id) {
        return documents.containsKey(id);
    }

    @Override
    public @NonNull List<Document> similaritySearch(@NonNull String query, int k) {
        // 简化实现：返回前 k 个文档
        // 实际应用中应该使用嵌入模型计算相似度
        return documents.values().stream()
                .limit(k)
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull List<Document> similaritySearchByEmbedding(@NonNull List<Double> embedding, int k) {
        if (embeddings.isEmpty()) {
            return List.of();
        }

        // 计算相似度并排序
        return embeddings.entrySet().stream()
                .map(entry -> {
                    String docId = entry.getKey();
                    List<Double> docEmbedding = entry.getValue();
                    double similarity = cosineSimilarity(embedding, docEmbedding);
                    Document doc = documents.get(docId);
                    return new SearchResult(doc, similarity);
                })
                .sorted((a, b) -> Double.compare(b.similarity, a.similarity))
                .limit(k)
                .map(SearchResult::document)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return documents.size();
    }

    @Override
    public void clear() {
        documents.clear();
        embeddings.clear();
    }

    /**
     * 计算余弦相似度
     */
    private double cosineSimilarity(@NonNull List<Double> a, @NonNull List<Double> b) {
        if (a.size() != b.size()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 搜索结果
     */
    private record SearchResult(Document document, double similarity) {}
}