package io.github.afgprojects.framework.ai.core.api.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Service for managing knowledge bases in a RAG system.
 *
 * <p>A knowledge base is a logical grouping of documents and their chunks,
 * typically representing a specific domain or topic.
 *
 * <p>Example usage:
 * <pre>{@code
 * KnowledgeBaseService service = ...;
 *
 * // Create a knowledge base
 * KnowledgeBaseInfo kb = service.create("Product Docs", "Product documentation");
 *
 * // Add documents to the knowledge base
 * service.addDocument(kb.id(), "doc-001", "Product content...", Map.of("source", "pdf"));
 *
 * // Search within a knowledge base
 * List<Document> results = service.search(kb.id(), "How to install?", 5, 0.7);
 *
 * // List all knowledge bases
 * List<KnowledgeBaseInfo> all = service.listAll();
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface KnowledgeBaseService {

    /**
     * Creates a new knowledge base.
     *
     * @param name        the knowledge base name, must not be null
     * @param description the description, may be null
     * @return the created knowledge base information
     */
    @NonNull
    KnowledgeBaseInfo create(@NonNull String name, @Nullable String description);

    /**
     * Creates a new knowledge base with metadata.
     *
     * @param name        the knowledge base name, must not be null
     * @param description the description, may be null
     * @param metadata    additional metadata, may be null
     * @return the created knowledge base information
     */
    @NonNull
    default KnowledgeBaseInfo create(@NonNull String name, @Nullable String description,
                                     @Nullable Map<String, Object> metadata) {
        return create(name, description);
    }

    /**
     * Retrieves a knowledge base by its ID.
     *
     * @param knowledgeBaseId the knowledge base ID, must not be null
     * @return the knowledge base information, or null if not found
     */
    @Nullable
    KnowledgeBaseInfo getById(@NonNull String knowledgeBaseId);

    /**
     * Lists all knowledge bases.
     *
     * @return the list of all knowledge bases
     */
    @NonNull
    List<KnowledgeBaseInfo> listAll();

    /**
     * Updates a knowledge base.
     *
     * @param knowledgeBaseId the knowledge base ID, must not be null
     * @param name            the new name, may be null to keep existing
     * @param description     the new description, may be null to keep existing
     * @return the updated knowledge base information
     */
    @NonNull
    KnowledgeBaseInfo update(@NonNull String knowledgeBaseId, @Nullable String name, @Nullable String description);

    /**
     * Deletes a knowledge base and all its associated documents.
     *
     * @param knowledgeBaseId the knowledge base ID, must not be null
     */
    void delete(@NonNull String knowledgeBaseId);

    /**
     * Adds a document to a knowledge base.
     *
     * <p>The document content will be chunked and embedded automatically.
     *
     * @param knowledgeBaseId the knowledge base ID, must not be null
     * @param documentId      the document ID, must not be null
     * @param content         the document content, must not be null
     * @param metadata        additional metadata, may be null
     */
    void addDocument(@NonNull String knowledgeBaseId, @NonNull String documentId,
                     @NonNull String content, @Nullable Map<String, Object> metadata);

    /**
     * Removes a document from a knowledge base.
     *
     * @param knowledgeBaseId the knowledge base ID, must not be null
     * @param documentId      the document ID, must not be null
     */
    void removeDocument(@NonNull String knowledgeBaseId, @NonNull String documentId);

    /**
     * Searches for documents within a knowledge base.
     *
     * @param knowledgeBaseId     the knowledge base ID, must not be null
     * @param query               the search query, must not be null
     * @param topK                the maximum number of results
     * @param similarityThreshold the minimum similarity score (0.0 to 1.0)
     * @return the list of matching documents
     */
    @NonNull
    List<Document> search(@NonNull String knowledgeBaseId, @NonNull String query,
                          int topK, double similarityThreshold);

    /**
     * Information about a knowledge base.
     *
     * @param id          the unique identifier
     * @param name        the knowledge base name
     * @param description the description
     * @param documentCount the number of documents in the knowledge base
     * @param metadata    additional metadata
     */
    record KnowledgeBaseInfo(
        @NonNull String id,
        @NonNull String name,
        @Nullable String description,
        long documentCount,
        @NonNull Map<String, Object> metadata
    ) {}
}
