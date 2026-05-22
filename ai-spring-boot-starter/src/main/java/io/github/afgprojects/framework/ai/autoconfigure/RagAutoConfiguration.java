package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.rag.*;
import io.github.afgprojects.framework.ai.rag.embedding.SpringAiEmbeddingModelAdapter;
import io.github.afgprojects.framework.ai.rag.loader.CompositeDocumentLoader;
import io.github.afgprojects.framework.ai.rag.loader.DefaultEncodingDetector;
import io.github.afgprojects.framework.ai.rag.loader.EncodingDetector;
import io.github.afgprojects.framework.ai.rag.retriever.SimpleRetriever;
import io.github.afgprojects.framework.ai.rag.splitter.RecursiveCharacterTextSplitter;
import io.github.afgprojects.framework.ai.rag.store.SimpleVectorStore;
import io.github.afgprojects.framework.ai.rag.store.SpringAiVectorStoreAdapter;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * RAG 模块自动配置
 *
 * <p>自动配置 RAG 模块的核心组件：
 * <ul>
 *   <li>DocumentLoader - 文档加载器</li>
 *   <li>TextSplitter - 文本切片器</li>
 *   <li>EmbeddingModel - 嵌入模型</li>
 *   <li>VectorStore - 向量存储</li>
 *   <li>Retriever - 检索器</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     rag:
 *       enabled: true
 *       splitter:
 *         type: recursive
 *         chunk-size: 500
 *         chunk-overlap: 50
 *       embedding:
 *         provider: ollama
 *         base-url: http://localhost:11434
 *         model: nomic-embed-text
 *         dimension: 768
 *       store:
 *         type: pgvector
 *         table-name: ai_vectors
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiAutoConfiguration.class)
@EnableConfigurationProperties(RagProperties.class)
@ConditionalOnProperty(prefix = "afg.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "afg.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RagAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RagAutoConfiguration.class);

    /**
     * 配置编码检测器
     */
    @Bean
    @ConditionalOnMissingBean(EncodingDetector.class)
    public EncodingDetector encodingDetector() {
        log.info("Creating default encoding detector");
        return new DefaultEncodingDetector();
    }

    /**
     * 配置文档加载器
     */
    @Bean
    @ConditionalOnMissingBean(DocumentLoader.class)
    public DocumentLoader documentLoader(@NonNull RagProperties properties) {
        log.info("Creating composite document loader");
        return new CompositeDocumentLoader();
    }

    /**
     * 配置文本切片器
     */
    @Bean
    @ConditionalOnMissingBean(TextSplitter.class)
    public TextSplitter textSplitter(@NonNull RagProperties properties) {
        RagProperties.SplitterConfig config = properties.getSplitter();
        int chunkSize = config.getChunkSize();
        int chunkOverlap = config.getChunkOverlap();

        log.info("Creating {} text splitter with chunkSize={}, chunkOverlap={}",
            config.getType(), chunkSize, chunkOverlap);

        return switch (config.getType()) {
            case RECURSIVE -> new RecursiveCharacterTextSplitter(chunkSize, chunkOverlap);
            case MARKDOWN -> new RecursiveCharacterTextSplitter(chunkSize, chunkOverlap);
            case TOKEN -> RecursiveCharacterTextSplitter.forTokenCount(
                chunkSize, chunkOverlap, text -> text.length() / 4
            );
        };
    }

    /**
     * 配置 Spring AI EmbeddingModel 适配器
     *
     * <p>当 Spring AI 的 EmbeddingModel Bean 存在时，自动创建适配器。
     * 支持所有 Spring AI 兼容的嵌入模型实现，包括：
     * <ul>
     *   <li>Ollama (spring-ai-ollama)</li>
     *   <li>OpenAI (spring-ai-openai)</li>
     *   <li>其他 Spring AI 支持的模型</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    @ConditionalOnBean(org.springframework.ai.embedding.EmbeddingModel.class)
    public EmbeddingModel springAiEmbeddingModelAdapter(
            org.springframework.ai.embedding.@NonNull EmbeddingModel springAiEmbeddingModel,
            @NonNull RagProperties properties) {
        RagProperties.EmbeddingConfig config = properties.getEmbedding();

        log.info("Creating Spring AI EmbeddingModel adapter: dimension={}", config.getDimension());

        return new SpringAiEmbeddingModelAdapter(springAiEmbeddingModel, config.getDimension());
    }

    /**
     * 配置简单向量存储（内存）
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnProperty(prefix = "afg.ai.rag.store", name = "type", havingValue = "simple", matchIfMissing = true)
    public VectorStore simpleVectorStore() {
        log.info("Creating simple in-memory vector store");
        return new SimpleVectorStore();
    }

    /**
     * 配置 Spring AI VectorStore 适配器
     *
     * <p>当 Spring AI 的 VectorStore Bean 存在时，自动创建适配器。
     * 支持所有 Spring AI 提供的向量存储实现，如：
     * <ul>
     *   <li>PgVectorStore - PostgreSQL pgvector</li>
     *   <li>ChromaVectorStore - Chroma</li>
     *   <li>PineconeVectorStore - Pinecone</li>
     *   <li>MilvusVectorStore - Milvus</li>
     *   <li>WeaviateVectorStore - Weaviate</li>
     *   <li>RedisVectorStore - Redis</li>
     * </ul>
     */
    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    @ConditionalOnBean(org.springframework.ai.vectorstore.VectorStore.class)
    public VectorStore springAiVectorStoreAdapter(
            org.springframework.ai.vectorstore.@NonNull VectorStore springAiVectorStore) {
        log.info("Creating Spring AI VectorStore adapter: type={}", springAiVectorStore.getClass().getSimpleName());

        return new SpringAiVectorStoreAdapter(springAiVectorStore);
    }

    /**
     * 配置检索器
     */
    @Bean
    @ConditionalOnMissingBean(Retriever.class)
    @ConditionalOnBean({VectorStore.class, EmbeddingModel.class})
    public Retriever retriever(
            @NonNull VectorStore vectorStore,
            @NonNull EmbeddingModel embeddingModel,
            @NonNull RagProperties properties) {
        int topK = properties.getRetriever().getTopK();

        log.info("Creating retriever with topK={}", topK);

        return new SimpleRetriever(vectorStore, embeddingModel, topK);
    }
}
