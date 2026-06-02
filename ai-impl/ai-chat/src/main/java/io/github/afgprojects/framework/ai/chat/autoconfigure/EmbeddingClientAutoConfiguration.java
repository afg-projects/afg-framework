package io.github.afgprojects.framework.ai.chat.autoconfigure;

import io.github.afgprojects.framework.ai.core.chat.DefaultEmbeddingClientRegistry;
import io.github.afgprojects.framework.ai.chat.SpringAiEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.chat.EmbeddingClientRegistry;
import io.github.afgprojects.framework.ai.core.api.model.DefaultModelInfo;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelType;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * EmbeddingClient 自动配置
 *
 * <p>当 classpath 上存在 {@link EmbeddingModel} 且 {@code afg.ai.embedding.enabled=true} 时自动激活。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnClass(EmbeddingModel.class)
@ConditionalOnProperty(prefix = "afg.ai.embedding", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(EmbeddingClientProperties.class)
public class EmbeddingClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(EmbeddingClientRegistry.class)
    public EmbeddingClientRegistry embeddingClientRegistry() {
        return new DefaultEmbeddingClientRegistry();
    }

    @Bean
    @ConditionalOnMissingBean(AfgEmbeddingClient.class)
    @ConditionalOnBean(EmbeddingModel.class)
    public AfgEmbeddingClient defaultAfgEmbeddingClient(EmbeddingModel embeddingModel) {
        return new SpringAiEmbeddingClient(embeddingModel);
    }

    @Bean
    @ConditionalOnBean({EmbeddingModel.class, EmbeddingClientRegistry.class, ModelRegistry.class})
    public EmbeddingModelRegistrar embeddingModelRegistrar(
            EmbeddingModel embeddingModel,
            EmbeddingClientRegistry registry,
            ModelRegistry modelRegistry,
            EmbeddingClientProperties properties) {
        return new EmbeddingModelRegistrar(embeddingModel, registry, modelRegistry, properties);
    }

    static class EmbeddingModelRegistrar {

        EmbeddingModelRegistrar(
                EmbeddingModel embeddingModel,
                EmbeddingClientRegistry registry,
                ModelRegistry modelRegistry,
                EmbeddingClientProperties properties) {
            var client = new SpringAiEmbeddingClient(embeddingModel);
            var name = properties.getDefaultName() != null ? properties.getDefaultName() : "default-embedding";
            registry.register(name, client);

            var info = DefaultModelInfo.of(name, ModelType.EMBEDDING);
            modelRegistry.registerModel(name, info);
            modelRegistry.setDefault(name, ModelType.EMBEDDING);
        }
    }
}