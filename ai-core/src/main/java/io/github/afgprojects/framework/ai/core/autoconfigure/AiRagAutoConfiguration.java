package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
// import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
// import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI RAG 自动配置。
 *
 * <p>配置前缀：{@code afg.ai.rag}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiRagAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class RagConfiguration {

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public NoOpVectorStore noOpVectorStore() {
        //     return new NoOpVectorStore();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public SimpleEmbeddingService simpleEmbeddingService(AfgAiProperties properties) {
        //     return new SimpleEmbeddingService(properties.getRag());
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public SimpleKnowledgeBaseService simpleKnowledgeBaseService(VectorStore vectorStore, EmbeddingService embeddingService) {
        //     return new SimpleKnowledgeBaseService(vectorStore, embeddingService);
        // }
    }
}
