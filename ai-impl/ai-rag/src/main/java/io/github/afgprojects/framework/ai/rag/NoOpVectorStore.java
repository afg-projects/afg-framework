package io.github.afgprojects.framework.ai.rag;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * No-op implementation of {@link VectorStore} that stores nothing and returns empty results.
 *
 * <p>Used as a fallback when no vector store backend is available,
 * ensuring the RAG system can still function (without actual vector search).
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class NoOpVectorStore implements VectorStore {

    private final Map<String, Document> store = new ConcurrentHashMap<>();

    @Override
    public void add(@NonNull Document document) {
        log.debug("NoOpVectorStore: adding document id={}", document.id());
        store.put(document.id(), document);
    }

    @Override
    @NonNull
    public List<Document> search(@NonNull String query, int topK, double similarityThreshold) {
        log.debug("NoOpVectorStore: search query='{}', topK={}, threshold={}", query, topK, similarityThreshold);
        return List.of();
    }

    @Override
    @NonNull
    public List<Document> search(@NonNull String query, int topK, double similarityThreshold,
                                 @Nullable Map<String, Object> filter) {
        return search(query, topK, similarityThreshold);
    }

    @Override
    public void delete(@NonNull String documentId) {
        log.debug("NoOpVectorStore: deleting document id={}", documentId);
        store.remove(documentId);
    }

    @Override
    public boolean exists(@NonNull String documentId) {
        return store.containsKey(documentId);
    }
}
