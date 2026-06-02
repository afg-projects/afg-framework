package io.github.afgprojects.framework.ai.core.api.rag;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a document in a RAG (Retrieval-Augmented Generation) system.
 * <p>
 * Document is the fundamental unit of information in RAG workflows:
 * <ul>
 *   <li>id - unique identifier for the document</li>
 *   <li>content - the text content of the document</li>
 *   <li>embedding - vector representation of the content</li>
 *   <li>metadata - additional key-value metadata</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create a document with content
 * Document doc = new Document("This is the document content.");
 *
 * // Create with metadata
 * Document doc = new Document(
 *     "doc-001",
 *     "Document content here",
 *     null,
 *     Map.of("source", "web", "author", "John")
 * );
 *
 * // Create with embedding
 * Document doc = new Document(
 *     "doc-001",
 *     "Content",
 *     List.of(0.1, 0.2, 0.3, ...),
 *     Map.of("category", "tech")
 * );
 * }</pre>
 *
 * @param id        the unique identifier
 * @param content   the text content
 * @param embedding the vector embedding (may be null)
 * @param metadata  the metadata map
 * @author AFG Projects
 * @since 1.0.0
 */
public record Document(
    @NonNull String id,
    @NonNull String content,
    @Nullable List<Double> embedding,
    @NonNull Map<String, Object> metadata
) {

    /**
     * Creates a Document with auto-generated ID and empty metadata.
     *
     * @param content the text content
     * @throws IllegalArgumentException if content is null or blank
     */
    public Document(@NonNull String content) {
        this(UUID.randomUUID().toString(), content, null, new HashMap<>());
    }

    /**
     * Creates a Document with auto-generated ID and specified metadata.
     *
     * @param content  the text content
     * @param metadata the metadata map
     * @throws IllegalArgumentException if content is null or blank
     */
    public Document(@NonNull String content, @NonNull Map<String, Object> metadata) {
        this(UUID.randomUUID().toString(), content, null, metadata);
    }

    /**
     * Creates a Document with validated parameters.
     * <p>
     * Null safety is ensured:
     * <ul>
     *   <li>id cannot be null or blank</li>
     *   <li>content cannot be null or blank</li>
     *   <li>metadata defaults to empty map if null</li>
     * </ul>
     *
     * @param id        the unique identifier
     * @param content   the text content
     * @param embedding the vector embedding
     * @param metadata  the metadata map
     * @throws IllegalArgumentException if id or content is null or blank
     */
    public Document {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content cannot be null or blank");
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    /**
     * Creates a Document with content only.
     *
     * @param content the text content
     * @return a new Document instance
     */
    @NonNull
    public static Document of(@NonNull String content) {
        return new Document(content);
    }

    /**
     * Creates a Document with content and metadata.
     *
     * @param content  the text content
     * @param metadata the metadata map
     * @return a new Document instance
     */
    @NonNull
    public static Document of(@NonNull String content, @NonNull Map<String, Object> metadata) {
        return new Document(content, metadata);
    }

    /**
     * Creates a Document with all parameters.
     *
     * @param id        the unique identifier
     * @param content   the text content
     * @param embedding the vector embedding
     * @param metadata  the metadata map
     * @return a new Document instance
     */
    @NonNull
    public static Document of(
        @NonNull String id,
        @NonNull String content,
        @Nullable List<Double> embedding,
        @NonNull Map<String, Object> metadata
    ) {
        return new Document(id, content, embedding, metadata);
    }

    /**
     * Creates a new Document with a different ID.
     *
     * @param id the new ID
     * @return a new Document instance
     */
    @NonNull
    public Document withId(@NonNull String id) {
        return new Document(id, this.content, this.embedding, this.metadata);
    }

    /**
     * Creates a new Document with different content.
     *
     * @param content the new content
     * @return a new Document instance
     */
    @NonNull
    public Document withContent(@NonNull String content) {
        return new Document(this.id, content, this.embedding, this.metadata);
    }

    /**
     * Creates a new Document with a different embedding.
     *
     * @param embedding the new embedding
     * @return a new Document instance
     */
    @NonNull
    public Document withEmbedding(@Nullable List<Double> embedding) {
        return new Document(this.id, this.content, embedding, this.metadata);
    }

    /**
     * Creates a new Document with additional metadata.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return a new Document instance
     */
    @NonNull
    public Document withMetadata(@NonNull String key, @Nullable Object value) {
        Map<String, Object> newMetadata = new HashMap<>(this.metadata);
        newMetadata.put(key, value);
        return new Document(this.id, this.content, this.embedding, newMetadata);
    }

    /**
     * Checks if this document has an embedding.
     *
     * @return true if embedding is present
     */
    public boolean hasEmbedding() {
        return embedding != null && !embedding.isEmpty();
    }

    /**
     * Gets a metadata value by key.
     *
     * @param key the metadata key
     * @return the value, or null if not present
     */
    @Nullable
    public Object getMetadata(@NonNull String key) {
        return metadata.get(key);
    }

    /**
     * Gets a metadata value by key with a default.
     *
     * @param key          the metadata key
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the value, or defaultValue if not present
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <T> T getMetadata(@NonNull String key, @NonNull T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }
}
