package io.github.afgprojects.framework.ai.rag.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import io.github.afgprojects.framework.ai.rag.NoOpVectorStore;
import io.github.afgprojects.framework.ai.rag.SimpleEmbeddingService;
import io.github.afgprojects.framework.ai.rag.SimpleKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for RAG (Retrieval-Augmented Generation) support.
 *
 * <p>Configures the core RAG components: embedding service, vector store, and knowledge base service.
 *
 * <p>Configuration:
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
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(RagProperties.class)
@ConditionalOnClass({VectorStore.class, EmbeddingService.class, KnowledgeBaseService.class})
@ConditionalOnProperty(prefix = "afg.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagAutoConfiguration {

    /**
     * Simple embedding service that generates random vectors.
     *
     * <p>For production use, provide a real {@link EmbeddingService} bean
     * (e.g., using OpenAI or Ollama embeddings).
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingService.class)
    public EmbeddingService embeddingService(RagProperties properties) {
        int dimensions = properties.getEmbedding().getDimensions();
        log.info("Creating SimpleEmbeddingService with dimensions={} (development only)", dimensions);
        return new SimpleEmbeddingService(dimensions);
    }

    /**
     * No-op vector store fallback.
     *
     * <p>For production use, provide a real {@link VectorStore} bean
     * (e.g., using PgVector, Milvus, or Chroma).
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore() {
        log.info("Creating NoOpVectorStore (no vector store backend configured)");
        return new NoOpVectorStore();
    }

    /**
     * Simple in-memory knowledge base service.
     *
     * <p>For production use with persistence, provide a JDBC-backed
     * {@link KnowledgeBaseService} bean.
     */
    @Bean
    @ConditionalOnMissingBean(KnowledgeBaseService.class)
    public KnowledgeBaseService knowledgeBaseService(VectorStore vectorStore,
                                                      EmbeddingService embeddingService) {
        log.info("Creating SimpleKnowledgeBaseService");
        return new SimpleKnowledgeBaseService(vectorStore, embeddingService);
    }
}
