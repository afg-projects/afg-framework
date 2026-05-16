package io.github.afgprojects.framework.ai.core.rag;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Interface for loading documents from various sources.
 * <p>
 * DocumentLoader is responsible for reading documents from external sources
 * such as files, URLs, databases, or other storage systems.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Load from file
 * List<Document> docs = loader.load(Source.ofFile("/path/to/document.pdf"));
 *
 * // Load from URL
 * List<Document> docs = loader.load(Source.ofUrl("https://example.com/article.html"));
 *
 * // Load from custom source
 * List<Document> docs = loader.load(new DatabaseSource("jdbc:postgresql://..."));
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentLoader {

    /**
     * Loads documents from the specified source.
     * <p>
     * The returned documents may or may not have embeddings set.
     * Use an {@link EmbeddingModel} to generate embeddings if needed.
     *
     * @param source the source to load from
     * @return the list of loaded documents
     * @throws IllegalArgumentException if source is null
     * @throws io.github.afgprojects.framework.ai.core.exception.AiException if loading fails
     */
    @NonNull
    List<Document> load(@NonNull Source source);

    /**
     * Checks if this loader supports the given source type.
     *
     * @param source the source to check
     * @return true if this loader can handle the source
     */
    boolean supports(@NonNull Source source);

    /**
     * Represents a source for document loading.
     * <p>
     * Source provides a unified abstraction for different document sources:
     * <ul>
     *   <li>File sources - local files</li>
     *   <li>URL sources - web resources</li>
     *   <li>Custom sources - databases, APIs, etc.</li>
     * </ul>
     *
     * @author AFG Projects
     * @since 1.0.0
     */
    interface Source {

        /**
         * Gets the path or identifier for this source.
         * <p>
         * The format depends on the source type:
         * <ul>
         *   <li>File: absolute or relative file path</li>
         *   <li>URL: the full URL string</li>
         *   <li>Custom: implementation-specific identifier</li>
         * </ul>
         *
         * @return the source path
         */
        @NonNull
        String getPath();

        /**
         * Gets the type of this source.
         *
         * @return the source type
         */
        @NonNull
        SourceType getType();

        /**
         * Gets the content type (MIME type) if known.
         *
         * @return the content type, or null if unknown
         */
        default String getContentType() {
            return null;
        }

        /**
         * Creates a file source.
         *
         * @param path the file path
         * @return a new file source
         */
        @NonNull
        static Source ofFile(@NonNull String path) {
            return new FileSource(path);
        }

        /**
         * Creates a URL source.
         *
         * @param url the URL string
         * @return a new URL source
         */
        @NonNull
        static Source ofUrl(@NonNull String url) {
            return new UrlSource(url);
        }

        /**
         * Creates a text source with inline content.
         *
         * @param content the text content
         * @return a new text source
         */
        @NonNull
        static Source ofText(@NonNull String content) {
            return new TextSource(content);
        }
    }

    /**
     * File-based document source.
     */
    record FileSource(@NonNull String path) implements Source {
        @Override
        public @NonNull String getPath() {
            return path;
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.FILE;
        }

        @Override
        public String getContentType() {
            // Infer from file extension
            int dotIndex = path.lastIndexOf('.');
            if (dotIndex > 0) {
                String ext = path.substring(dotIndex + 1).toLowerCase();
                return switch (ext) {
                    case "pdf" -> "application/pdf";
                    case "txt" -> "text/plain";
                    case "md", "markdown" -> "text/markdown";
                    case "html", "htm" -> "text/html";
                    case "json" -> "application/json";
                    case "xml" -> "application/xml";
                    case "csv" -> "text/csv";
                    default -> "application/octet-stream";
                };
            }
            return "application/octet-stream";
        }
    }

    /**
     * URL-based document source.
     */
    record UrlSource(@NonNull String path) implements Source {
        @Override
        public @NonNull String getPath() {
            return path;
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.URL;
        }
    }

    /**
     * Inline text content source.
     */
    record TextSource(@NonNull String content) implements Source {
        @Override
        public @NonNull String getPath() {
            return "inline://" + Integer.toHexString(content.hashCode());
        }

        @Override
        public @NonNull SourceType getType() {
            return SourceType.TEXT;
        }

        @Override
        public String getContentType() {
            return "text/plain";
        }
    }

    /**
     * Enumeration of source types.
     */
    enum SourceType {
        FILE,
        URL,
        TEXT,
        DATABASE,
        API,
        CUSTOM
    }
}
