package io.github.afgprojects.framework.ai.spring.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.chat.EmbeddingClientRegistry;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.spring.config.SpringAiProperties;
import io.github.afgprojects.framework.ai.spring.model.DefaultModelRoutingService;
import io.github.afgprojects.framework.ai.spring.model.SpringAiModelRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Spring AI Model 自动配置
 *
 * <p>自动检测 classpath 上的 Spring AI 模型 Bean，注册到 AFG 的 {@link ModelRegistry} 中，
 * 并创建 {@link DefaultModelRoutingService} 用于模型路由。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(
    after = SpringAiChatAutoConfiguration.class,
    afterName = {
        "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration",
        "io.github.afgprojects.framework.ai.core.autoconfigure.AiCoreAutoConfiguration"
    }
)
@ConditionalOnClass(name = "org.springframework.ai.chat.model.ChatModel")
@ConditionalOnProperty(prefix = "afg.ai.spring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ModelRegistry.class)
    public SpringAiModelRegistry springAiModelRegistry(
            ObjectProvider<List<ChatModel>> chatModelsProvider,
            ObjectProvider<List<EmbeddingModel>> embeddingModelsProvider,
            SpringAiProperties properties) {
        SpringAiModelRegistry registry = new SpringAiModelRegistry();

        List<ChatModel> chatModels = chatModelsProvider.getIfAvailable(() -> List.of());
        if (chatModels != null) {
            for (int i = 0; i < chatModels.size(); i++) {
                String name = i == 0 ? properties.getDefaultChatModel() : "chat-model-" + i;
                registry.registerChatModel(name, chatModels.get(i), "spring-ai");
                if (i == 0) {
                    registry.setDefault(name, io.github.afgprojects.framework.ai.core.api.model.ModelType.CHAT);
                }
            }
        }

        List<EmbeddingModel> embeddingModels = embeddingModelsProvider.getIfAvailable(() -> List.of());
        if (embeddingModels != null) {
            for (int i = 0; i < embeddingModels.size(); i++) {
                String name = i == 0 ? properties.getDefaultEmbeddingModel() : "embedding-model-" + i;
                registry.registerEmbeddingModel(name, embeddingModels.get(i), "spring-ai");
                if (i == 0) {
                    registry.setDefault(name, io.github.afgprojects.framework.ai.core.api.model.ModelType.EMBEDDING);
                }
            }
        }

        log.info("SpringAiModelRegistry initialized with {} chat models, {} embedding models",
                chatModels != null ? chatModels.size() : 0,
                embeddingModels != null ? embeddingModels.size() : 0);

        return registry;
    }

    @Bean
    @ConditionalOnMissingBean(DefaultModelRoutingService.class)
    @ConditionalOnBean({ChatClientRegistry.class, EmbeddingClientRegistry.class, ModelRegistry.class})
    public DefaultModelRoutingService defaultModelRoutingService(
            ChatClientRegistry chatClientRegistry,
            EmbeddingClientRegistry embeddingClientRegistry,
            ModelRegistry modelRegistry) {
        log.info("Creating DefaultModelRoutingService");
        return new DefaultModelRoutingService(chatClientRegistry, embeddingClientRegistry, modelRegistry);
    }
}
