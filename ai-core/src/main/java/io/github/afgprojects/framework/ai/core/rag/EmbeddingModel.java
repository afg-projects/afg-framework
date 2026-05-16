package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for computing text embeddings.
 * <p>
 * EmbeddingModel converts text into vector representations (embeddings)
 * that capture semantic meaning. These vectors can be used for:
 * <ul>
 *   <li>Similarity search in vector stores</li>
 *   <li>Clustering and classification</li>
 *   <li>Semantic analysis</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Single text embedding
 * List<Double> embedding = model.embed("Hello, world!");
 *
 * // Batch embedding
 * List<List<Double>> embeddings = model.embedBatch(
 *     List.of("Text 1", "Text 2", "Text 3")
 * );
 *
 * // Streaming embedding
 * Flux<List<Double>> stream = model.embedStream(texts);
 * stream.subscribe(embedding -> store.add(doc.withEmbedding(embedding)));
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface EmbeddingModel {

    /**
     * Computes the embedding for a single text.
     *
     * @param text the text to embed
     * @return the embedding vector
     * @throws IllegalArgumentException if text is null or blank
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if embedding fails
     */
    @NonNull
    List<Double> embed(@NonNull String text);

    /**
     * Computes embeddings for multiple texts in batch.
     * <p>
     * Batch embedding is typically more efficient than individual
     * embedding calls, especially for large numbers of texts.
     *
     * @param texts the texts to embed
     * @return the list of embedding vectors, in the same order as input
     * @throws IllegalArgumentException if texts is null or empty
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if embedding fails
     */
    @NonNull
    List<List<Double>> embedBatch(@NonNull List<String> texts);

    /**
     * Computes embeddings for multiple texts as a stream.
     * <p>
     * This is useful for processing large numbers of texts
     * without loading all embeddings into memory at once.
     *
     * @param texts the texts to embed
     * @return a Flux of embedding vectors
     */
    @NonNull
    default Flux<List<Double>> embedStream(@NonNull List<String> texts) {
        return Flux.fromIterable(embedBatch(texts));
    }

    /**
     * Computes the embedding for a document.
     * <p>
     * This is a convenience method that embeds the document's content
     * and returns a new document with the embedding set.
     *
     * @param document the document to embed
     * @return a new document with the embedding
     */
    @NonNull
    default Document embedDocument(@NonNull Document document) {
        List<Double> embedding = embed(document.content());
        return document.withEmbedding(embedding);
    }

    /**
     * Computes embeddings for multiple documents.
     *
     * @param documents the documents to embed
     * @return the list of documents with embeddings
     */
    @NonNull
    default List<Document> embedDocuments(@NonNull List<Document> documents) {
        List<String> contents = documents.stream()
            .map(Document::content)
            .toList();
        List<List<Double>> embeddings = embedBatch(contents);

        List<Document> result = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            result.add(documents.get(i).withEmbedding(embeddings.get(i)));
        }
        return result;
    }

    /**
     * Gets the dimension of the embedding vectors.
     *
     * @return the embedding dimension
     */
    int dimension();

    /**
     * Gets the model name/identifier.
     *
     * @return the model name
     */
    @NonNull
    String modelName();

    /**
     * Computes the similarity between two embeddings.
     * <p>
     * Default implementation uses cosine similarity.
     *
     * @param embedding1 the first embedding
     * @param embedding2 the second embedding
     * @return the similarity score (typically between -1 and 1)
     * @throws IllegalArgumentException if embeddings have different dimensions
     */
    default double similarity(@NonNull List<Double> embedding1, @NonNull List<Double> embedding2) {
        if (embedding1.size() != embedding2.size()) {
            throw new IllegalArgumentException("Embeddings must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < embedding1.size(); i++) {
            double v1 = embedding1.get(i);
            double v2 = embedding2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Gets the maximum batch size supported by this model.
     *
     * @return the maximum batch size
     */
    default int maxBatchSize() {
        return 100;
    }
}
