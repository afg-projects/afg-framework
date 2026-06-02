package io.github.afgprojects.framework.ai.core.rag;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of {@link KnowledgeBaseService}.
 *
 * <p>This implementation stores knowledge base metadata in memory and delegates
 * document storage and search to a {@link VectorStore}.
 *
 * <p>For production use with persistence, configure a JDBC-backed implementation.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class SimpleKnowledgeBaseService implements KnowledgeBaseService {

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final Map<String, KnowledgeBaseInfo> knowledgeBases = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public KnowledgeBaseInfo create(@NonNull String name, @Nullable String description) {
        String id = UUID.randomUUID().toString();
        KnowledgeBaseInfo info = new KnowledgeBaseInfo(id, name, description, 0, new HashMap<>());
        knowledgeBases.put(id, info);
        log.info("Created knowledge base: id={}, name={}", id, name);
        return info;
    }

    @Override
    @NonNull
    public KnowledgeBaseInfo create(@NonNull String name, @Nullable String description,
                                    @Nullable Map<String, Object> metadata) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> meta = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        KnowledgeBaseInfo info = new KnowledgeBaseInfo(id, name, description, 0, meta);
        knowledgeBases.put(id, info);
        log.info("Created knowledge base: id={}, name={}", id, name);
        return info;
    }

    @Override
    @Nullable
    public KnowledgeBaseInfo getById(@NonNull String knowledgeBaseId) {
        return knowledgeBases.get(knowledgeBaseId);
    }

    @Override
    @NonNull
    public List<KnowledgeBaseInfo> listAll() {
        return List.copyOf(knowledgeBases.values());
    }

    @Override
    @NonNull
    public KnowledgeBaseInfo update(@NonNull String knowledgeBaseId, @Nullable String name,
                                    @Nullable String description) {
        KnowledgeBaseInfo existing = knowledgeBases.get(knowledgeBaseId);
        if (existing == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId);
        }
        KnowledgeBaseInfo updated = new KnowledgeBaseInfo(
            knowledgeBaseId,
            name != null ? name : existing.name(),
            description != null ? description : existing.description(),
            existing.documentCount(),
            existing.metadata()
        );
        knowledgeBases.put(knowledgeBaseId, updated);
        log.info("Updated knowledge base: id={}", knowledgeBaseId);
        return updated;
    }

    @Override
    public void delete(@NonNull String knowledgeBaseId) {
        knowledgeBases.remove(knowledgeBaseId);
        log.info("Deleted knowledge base: id={}", knowledgeBaseId);
    }

    @Override
    public void addDocument(@NonNull String knowledgeBaseId, @NonNull String documentId,
                            @NonNull String content, @Nullable Map<String, Object> metadata) {
        KnowledgeBaseInfo kb = knowledgeBases.get(knowledgeBaseId);
        if (kb == null) {
            throw new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId);
        }

        Map<String, Object> docMetadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        docMetadata.put("knowledgeBaseId", knowledgeBaseId);

        List<Double> embedding = embeddingService.embed(content);
        Document doc = Document.of(documentId, content, embedding, docMetadata);
        vectorStore.add(doc);

        // Update document count
        KnowledgeBaseInfo updated = new KnowledgeBaseInfo(
            kb.id(), kb.name(), kb.description(),
            kb.documentCount() + 1, kb.metadata()
        );
        knowledgeBases.put(knowledgeBaseId, updated);
        log.info("Added document to knowledge base: kbId={}, docId={}", knowledgeBaseId, documentId);
    }

    @Override
    public void removeDocument(@NonNull String knowledgeBaseId, @NonNull String documentId) {
        vectorStore.delete(documentId);

        KnowledgeBaseInfo kb = knowledgeBases.get(knowledgeBaseId);
        if (kb != null) {
            KnowledgeBaseInfo updated = new KnowledgeBaseInfo(
                kb.id(), kb.name(), kb.description(),
                Math.max(0, kb.documentCount() - 1), kb.metadata()
            );
            knowledgeBases.put(knowledgeBaseId, updated);
        }
        log.info("Removed document from knowledge base: kbId={}, docId={}", knowledgeBaseId, documentId);
    }

    @Override
    @NonNull
    public List<Document> search(@NonNull String knowledgeBaseId, @NonNull String query,
                                 int topK, double similarityThreshold) {
        return vectorStore.search(query, topK, similarityThreshold,
            Map.of("knowledgeBaseId", knowledgeBaseId));
    }
}
