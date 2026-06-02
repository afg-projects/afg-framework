package io.github.afgprojects.framework.ai.rag.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for RAG support.
 *
 * <p>Properties are bound to the prefix {@code afg.ai.rag}.
 *
 * <p>Example:
 * <pre>{@code
 * afg:
 *   ai:
 *     rag:
 *       enabled: true
 *       embedding:
 *         dimensions: 1536
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.ai.rag")
public class RagProperties {

    /**
     * Whether RAG support is enabled.
     */
    private boolean enabled = true;

    /**
     * Embedding configuration.
     */
    private Embedding embedding = new Embedding();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    /**
     * Embedding service configuration.
     */
    public static class Embedding {

        /**
         * Embedding vector dimensionality.
         *
         * <p>Default is 1536 (OpenAI text-embedding-ada-002 dimension).
         */
        private int dimensions = 1536;

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }
    }
}
