package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for splitting text into smaller chunks.
 * <p>
 * TextSplitter is used in RAG pipelines to break large documents
 * into smaller, more manageable chunks for embedding and retrieval.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Split text into chunks
 * TextSplitter splitter = new RecursiveCharacterTextSplitter(500, 50);
 * List<String> chunks = splitter.split(longText);
 *
 * // Use with document loader
 * List<Document> docs = loader.load(source);
 * List<Document> chunks = docs.stream()
 *     .flatMap(doc -> splitter.split(doc.content()).stream()
 *         .map(chunk -> doc.withContent(chunk)))
 *     .toList();
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface TextSplitter {

    /**
     * Splits the given text into chunks.
     *
     * @param text the text to split
     * @return the list of text chunks
     * @throws IllegalArgumentException if text is null
     */
    @NonNull
    List<String> split(@NonNull String text);

    /**
     * Splits a document into multiple chunk documents.
     * <p>
     * Each chunk will inherit the metadata from the original document
     * and have a unique ID derived from the original.
     *
     * @param document the document to split
     * @return the list of chunk documents
     * @throws IllegalArgumentException if document is null
     */
    @NonNull
    default List<Document> splitDocument(@NonNull Document document) {
        List<String> chunks = split(document.content());
        List<Document> result = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            // Create chunk document with derived ID
            Document chunkDoc = new Document(
                document.id() + "-chunk-" + i,
                chunk,
                null, // Embedding will be computed later
                document.metadata()
            );
            result.add(chunkDoc);
        }

        return result;
    }

    /**
     * Splits multiple documents into chunks.
     *
     * @param documents the documents to split
     * @return the list of all chunk documents
     */
    @NonNull
    default List<Document> splitDocuments(@NonNull List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            result.addAll(splitDocument(doc));
        }
        return result;
    }

    /**
     * Gets the target chunk size.
     *
     * @return the target size in characters
     */
    int getChunkSize();

    /**
     * Gets the overlap between chunks.
     *
     * @return the overlap size in characters
     */
    int getChunkOverlap();

    /**
     * Creates a simple character-based text splitter.
     *
     * @param chunkSize the target chunk size
     * @return a new text splitter
     */
    @NonNull
    static TextSplitter of(int chunkSize) {
        return new CharacterTextSplitter(chunkSize, 0);
    }

    /**
     * Creates a character-based text splitter with overlap.
     *
     * @param chunkSize    the target chunk size
     * @param chunkOverlap the overlap between chunks
     * @return a new text splitter
     */
    @NonNull
    static TextSplitter of(int chunkSize, int chunkOverlap) {
        return new CharacterTextSplitter(chunkSize, chunkOverlap);
    }

    /**
     * Simple character-based text splitter implementation.
     */
    record CharacterTextSplitter(int chunkSize, int chunkOverlap) implements TextSplitter {

        /**
         * Creates a CharacterTextSplitter with validated parameters.
         *
         * @param chunkSize    the target chunk size
         * @param chunkOverlap the overlap between chunks
         * @throws IllegalArgumentException if chunkSize is not positive
         */
        public CharacterTextSplitter {
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("chunkSize must be positive");
            }
            if (chunkOverlap < 0) {
                throw new IllegalArgumentException("chunkOverlap cannot be negative");
            }
            if (chunkOverlap >= chunkSize) {
                throw new IllegalArgumentException("chunkOverlap must be less than chunkSize");
            }
        }

        @Override
        public @NonNull List<String> split(@NonNull String text) {
            if (text.isEmpty()) {
                return List.of();
            }

            List<String> chunks = new ArrayList<>();
            int start = 0;

            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                chunks.add(text.substring(start, end));

                if (end == text.length()) {
                    break;
                }

                start = end - chunkOverlap;
            }

            return chunks;
        }

        @Override
        public int getChunkSize() {
            return chunkSize;
        }

        @Override
        public int getChunkOverlap() {
            return chunkOverlap;
        }
    }
}
