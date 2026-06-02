package io.github.afgprojects.framework.ai.core.api.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Abstraction for a vector store that supports storing, searching, and managing documents
 * with vector embeddings.
 *
 * <p>A VectorStore is the core component for RAG (Retrieval-Augmented Generation) systems,
 * enabling similarity search over document embeddings.
 *
 * <p>Example usage:
 * <pre>{@code
 * VectorStore store = ...;
 *
 * // Add documents
 * Document doc = Document.of("This is some content", Map.of("source", "web"));
 * store.add(doc);
 *
 * // Search for similar documents
 * List<Document> results = store.search("query text", 5, 0.7);
 *
 * // Delete by document ID
 * store.delete("doc-001");
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface VectorStore {

    /**
     * Adds a document to the vector store.
     *
     * <p>If the document does not have an embedding, the vector store implementation
     * should generate one using the configured {@link EmbeddingService}.
     *
     * @param document the document to add, must not be null
     */
    void add(@NonNull Document document);

    /**
     * Adds multiple documents to the vector store.
     *
     * <p>Default implementation calls {@link #add(Document)} for each document.
     * Implementations should override this for better batch performance.
     *
     * @param documents the documents to add, must not be null
     */
    default void addBatch(@NonNull List<Document> documents) {
        documents.forEach(this::add);
    }

    /**
     * Searches for documents similar to the given query text.
     *
     * @param query               the query text, must not be null
     * @param topK                the maximum number of results to return
     * @param similarityThreshold the minimum similarity score (0.0 to 1.0)
     * @return the list of matching documents, sorted by similarity (highest first)
     */
    @NonNull
    List<Document> search(@NonNull String query, int topK, double similarityThreshold);

    /**
     * Searches for documents similar to the given query text with metadata filter.
     *
     * <p>Default implementation ignores the filter and delegates to {@link #search(String, int, double)}.
     * Implementations should override this to support filtering.
     *
     * @param query               the query text, must not be null
     * @param topK                the maximum number of results to return
     * @param similarityThreshold the minimum similarity score (0.0 to 1.0)
     * @param filter              metadata filter criteria, may be null
     * @return the list of matching documents, sorted by similarity (highest first)
     */
    @NonNull
    default List<Document> search(@NonNull String query, int topK, double similarityThreshold,
                                  @Nullable Map<String, Object> filter) {
        return search(query, topK, similarityThreshold);
    }

    /**
     * Deletes a document by its ID.
     *
     * @param documentId the document ID, must not be null
     */
    void delete(@NonNull String documentId);

    /**
     * Deletes documents by their IDs.
     *
     * <p>Default implementation calls {@link #delete(String)} for each ID.
     *
     * @param documentIds the document IDs to delete, must not be null
     */
    default void deleteBatch(@NonNull List<String> documentIds) {
        documentIds.forEach(this::delete);
    }

    /**
     * Checks if the vector store contains a document with the given ID.
     *
     * @param documentId the document ID, must not be null
     * @return true if the document exists
     */
    boolean exists(@NonNull String documentId);
}
