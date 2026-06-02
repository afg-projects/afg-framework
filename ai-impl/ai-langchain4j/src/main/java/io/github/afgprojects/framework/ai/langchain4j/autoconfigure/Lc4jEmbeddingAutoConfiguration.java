package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.model.embedding.EmbeddingModel;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.langchain4j.chat.Lc4jEmbeddingClient;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * LangChain4j Embedding 自动配置
 *
 * <p>当 classpath 上存在 LangChain4j {@link EmbeddingModel} 且
 * {@code afg.ai.langchain4j.enabled=true} 时自动激活。
 * 注册 {@link Lc4jEmbeddingClient} 作为 {@link AfgEmbeddingClient} 的实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.model.embedding.EmbeddingModel")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jEmbeddingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AfgEmbeddingClient.class)
    @ConditionalOnBean(EmbeddingModel.class)
    public Lc4jEmbeddingClient lc4jEmbeddingClient(EmbeddingModel embeddingModel,
                                                    Lc4jProperties properties) {
        log.info("Creating Lc4jEmbeddingClient with defaultEmbeddingModel={}", properties.getDefaultEmbeddingModel());
        return new Lc4jEmbeddingClient(embeddingModel);
    }
}
