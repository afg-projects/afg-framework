package io.github.afgprojects.framework.ai.spring.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import io.github.afgprojects.framework.ai.spring.config.SpringAiProperties;
import io.github.afgprojects.framework.ai.spring.rag.SpringAiVectorStoreAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI RAG 自动配置
 *
 * <p>当 classpath 上存在 Spring AI {@link org.springframework.ai.vectorstore.VectorStore} 接口
 * 且 {@code afg.ai.spring.rag.enabled=true} 时自动激活。
 * 注册 {@link SpringAiVectorStoreAdapter} 作为 Spring AI VectorStore 的实现。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     spring:
 *       rag:
 *         enabled: true
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SpringAiProperties.class)
@ConditionalOnClass(name = "org.springframework.ai.vectorstore.VectorStore")
@ConditionalOnProperty(prefix = "afg.ai.spring.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiRagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(org.springframework.ai.vectorstore.VectorStore.class)
    @ConditionalOnBean({VectorStore.class, EmbeddingService.class})
    public SpringAiVectorStoreAdapter springAiVectorStoreAdapter(
            VectorStore afgVectorStore,
            EmbeddingService afgEmbeddingService,
            SpringAiProperties properties) {
        log.info("Creating SpringAiVectorStoreAdapter bridging AFG VectorStore + EmbeddingService");
        return new SpringAiVectorStoreAdapter(afgVectorStore, afgEmbeddingService);
    }
}
