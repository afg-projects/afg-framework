package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Interface for retrieving relevant documents for a query.
 * <p>
 * Retriever is the main entry point for RAG retrieval operations.
 * It combines vector search with optional re-ranking and filtering.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Simple retrieval
 * List<Document> docs = retriever.retrieve("What is machine learning?");
 *
 * // With result limit
 * List<Document> docs = retriever.retrieve(query, 10);
 *
 * // With filter
 * FilterExpression filter = FilterExpression.eq("category", "tech");
 * List<Document> docs = retriever.retrieve(query, filter);
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface Retriever {

    /**
     * Retrieves documents relevant to the query.
     *
     * @param query the query text
     * @return the list of relevant documents
     * @throws IllegalArgumentException if query is null or blank
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if retrieval fails
     */
    @NonNull
    List<Document> retrieve(@NonNull String query);

    /**
     * Retrieves documents with a limit on the number of results.
     *
     * @param query the query text
     * @param k     the maximum number of documents to return
     * @return the list of relevant documents
     * @throws IllegalArgumentException if query is null or blank, or k is negative
     */
    @NonNull
    default List<Document> retrieve(@NonNull String query, int k) {
        List<Document> all = retrieve(query);
        if (k <= 0 || k >= all.size()) {
            return all;
        }
        return all.subList(0, k);
    }

    /**
     * Retrieves documents with a filter expression.
     *
     * @param query  the query text
     * @param filter the filter expression
     * @return the list of relevant documents matching the filter
     */
    @NonNull
    default List<Document> retrieve(@NonNull String query, @Nullable FilterExpression filter) {
        // Default: ignore filter, implementations should override
        return retrieve(query);
    }

    /**
     * Retrieves documents with filter and limit.
     *
     * @param query  the query text
     * @param k      the maximum number of documents
     * @param filter the filter expression
     * @return the list of relevant documents
     */
    @NonNull
    default List<Document> retrieve(
        @NonNull String query,
        int k,
        @Nullable FilterExpression filter
    ) {
        List<Document> all = retrieve(query, filter);
        if (k <= 0 || k >= all.size()) {
            return all;
        }
        return all.subList(0, k);
    }

    /**
     * Retrieves documents and returns them with relevance scores.
     *
     * @param query the query text
     * @return the list of documents with scores
     */
    @NonNull
    default List<ScoredDocument> retrieveWithScores(@NonNull String query) {
        // Default: return documents with default score of 1.0
        return retrieve(query).stream()
            .map(doc -> new ScoredDocument(doc, 1.0))
            .toList();
    }

    /**
     * Retrieves documents with scores and limit.
     *
     * @param query the query text
     * @param k     the maximum number of documents
     * @return the list of scored documents
     */
    @NonNull
    default List<ScoredDocument> retrieveWithScores(@NonNull String query, int k) {
        List<ScoredDocument> all = retrieveWithScores(query);
        if (k <= 0 || k >= all.size()) {
            return all;
        }
        return all.subList(0, k);
    }

    /**
     * Gets the vector store used by this retriever.
     *
     * @return the vector store
     */
    @NonNull
    VectorStore getVectorStore();

    /**
     * Gets the embedding model used by this retriever.
     *
     * @return the embedding model
     */
    @NonNull
    EmbeddingModel getEmbeddingModel();

    /**
     * A document with a relevance score.
     *
     * @param document the document
     * @param score    the relevance score (typically 0-1)
     */
    record ScoredDocument(@NonNull Document document, double score) {
        /**
         * Creates a ScoredDocument with validated score.
         *
         * @param document the document
         * @param score    the relevance score
         */
        public ScoredDocument {
            if (document == null) {
                throw new IllegalArgumentException("document cannot be null");
            }
        }
    }
}
