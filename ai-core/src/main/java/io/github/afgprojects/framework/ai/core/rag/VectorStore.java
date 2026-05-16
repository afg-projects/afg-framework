package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Interface for vector storage operations in a RAG system.
 * <p>
 * VectorStore provides methods for:
 * <ul>
 *   <li>Adding documents with embeddings</li>
 *   <li>Similarity search using vector embeddings</li>
 *   <li>Updating and deleting documents</li>
 *   <li>Filtered similarity search</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Add documents
 * List<Document> docs = List.of(
 *     new Document("doc-1", "Content 1", embedding1, metadata1),
 *     new Document("doc-2", "Content 2", embedding2, metadata2)
 * );
 * vectorStore.add(docs);
 *
 * // Similarity search
 * List<Document> results = vectorStore.similaritySearch("query text", 5);
 *
 * // Filtered search
 * FilterExpression filter = FilterExpression.eq("category", "tech");
 * List<Document> results = vectorStore.similaritySearch("query", 5, filter);
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface VectorStore {

    /**
     * Adds documents to the vector store.
     * <p>
     * Documents should have embeddings set before being added.
     * If embeddings are missing, implementations may throw an exception
     * or compute embeddings automatically.
     *
     * @param documents the documents to add
     * @throws IllegalArgumentException if documents is null or empty
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the operation fails
     */
    void add(@NonNull List<Document> documents);

    /**
     * Performs a similarity search using a query string.
     * <p>
     * The query string will be converted to an embedding using
     * the configured embedding model, then used to find similar documents.
     *
     * @param query the query text
     * @param k     the number of results to return
     * @return the list of most similar documents, ordered by relevance
     * @throws IllegalArgumentException if query is null or blank, or k is negative
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the search fails
     */
    @NonNull
    List<Document> similaritySearch(@NonNull String query, int k);

    /**
     * Performs a similarity search with a filter expression.
     * <p>
     * The filter expression is used to pre-filter documents before
     * performing the similarity search.
     *
     * @param query  the query text
     * @param k      the number of results to return
     * @param filter the filter expression to apply
     * @return the list of most similar documents matching the filter
     * @throws IllegalArgumentException if query is null or blank
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the search fails
     */
    @NonNull
    default List<Document> similaritySearch(
        @NonNull String query,
        int k,
        @Nullable FilterExpression filter
    ) {
        // Default implementation: ignore filter
        // Implementations should override this for proper filtering
        return similaritySearch(query, k);
    }

    /**
     * Performs a similarity search using a pre-computed embedding.
     *
     * @param embedding the query embedding
     * @param k         the number of results to return
     * @return the list of most similar documents
     * @throws IllegalArgumentException if embedding is null or empty
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the search fails
     */
    @NonNull
    List<Document> similaritySearchByEmbedding(@NonNull List<Double> embedding, int k);

    /**
     * Performs a similarity search with embedding and filter.
     *
     * @param embedding the query embedding
     * @param k         the number of results to return
     * @param filter    the filter expression to apply
     * @return the list of most similar documents matching the filter
     */
    @NonNull
    default List<Document> similaritySearchByEmbedding(
        @NonNull List<Double> embedding,
        int k,
        @Nullable FilterExpression filter
    ) {
        return similaritySearchByEmbedding(embedding, k);
    }

    /**
     * Deletes documents by their IDs.
     *
     * @param ids the document IDs to delete
     * @throws IllegalArgumentException if ids is null or empty
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the operation fails
     */
    void delete(@NonNull List<String> ids);

    /**
     * Deletes a single document by ID.
     *
     * @param id the document ID
     */
    default void delete(@NonNull String id) {
        delete(List.of(id));
    }

    /**
     * Updates a document in the store.
     * <p>
     * The document must already exist in the store.
     *
     * @param document the document to update
     * @throws IllegalArgumentException if document is null
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if the operation fails
     */
    void update(@NonNull Document document);

    /**
     * Gets a document by ID.
     *
     * @param id the document ID
     * @return the document, or null if not found
     */
    @Nullable
    Document getById(@NonNull String id);

    /**
     * Checks if a document exists.
     *
     * @param id the document ID
     * @return true if the document exists
     */
    boolean exists(@NonNull String id);

    /**
     * Gets the total number of documents in the store.
     *
     * @return the document count
     */
    long count();

    /**
     * Clears all documents from the store.
     */
    void clear();
}
