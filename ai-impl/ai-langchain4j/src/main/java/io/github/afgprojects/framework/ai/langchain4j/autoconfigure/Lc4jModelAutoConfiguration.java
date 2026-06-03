package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.github.afgprojects.framework.ai.core.api.model.ModelRegistry;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import io.github.afgprojects.framework.ai.langchain4j.model.Lc4jModelRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * LangChain4j Model 自动配置
 *
 * <p>自动检测 classpath 上的 LangChain4j 模型 Bean，注册到 AFG 的 {@link ModelRegistry} 中。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = Lc4jChatAutoConfiguration.class)
@ConditionalOnClass(name = "dev.langchain4j.model.chat.ChatLanguageModel")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ModelRegistry.class)
    public Lc4jModelRegistry lc4jModelRegistry(
            ObjectProvider<List<ChatLanguageModel>> chatModelsProvider,
            ObjectProvider<List<StreamingChatLanguageModel>> streamingChatModelsProvider,
            ObjectProvider<List<EmbeddingModel>> embeddingModelsProvider,
            Lc4jProperties properties) {
        Lc4jModelRegistry registry = new Lc4jModelRegistry();

        // 注册 ChatLanguageModel
        List<ChatLanguageModel> chatModels = chatModelsProvider.getIfAvailable(() -> List.of());
        if (chatModels != null) {
            for (int i = 0; i < chatModels.size(); i++) {
                String name = i == 0 ? properties.getDefaultChatModel() : "chat-model-" + i;
                registry.registerChatModel(name, chatModels.get(i), "langchain4j");
                if (i == 0) {
                    registry.setDefault(name, io.github.afgprojects.framework.ai.core.api.model.ModelType.CHAT);
                }
            }
        }

        // 注册 StreamingChatLanguageModel
        List<StreamingChatLanguageModel> streamingChatModels = streamingChatModelsProvider.getIfAvailable(() -> List.of());
        if (streamingChatModels != null) {
            for (int i = 0; i < streamingChatModels.size(); i++) {
                String name = "streaming-" + (i == 0 ? properties.getDefaultChatModel() : "chat-model-" + i);
                registry.registerStreamingChatModel(name, streamingChatModels.get(i), "langchain4j");
            }
        }

        // 注册 EmbeddingModel
        List<EmbeddingModel> embeddingModels = embeddingModelsProvider.getIfAvailable(() -> List.of());
        if (embeddingModels != null) {
            for (int i = 0; i < embeddingModels.size(); i++) {
                String name = i == 0 ? properties.getDefaultEmbeddingModel() : "embedding-model-" + i;
                registry.registerEmbeddingModel(name, embeddingModels.get(i), "langchain4j");
                if (i == 0) {
                    registry.setDefault(name, io.github.afgprojects.framework.ai.core.api.model.ModelType.EMBEDDING);
                }
            }
        }

        log.info("Lc4jModelRegistry initialized with {} chat models, {} streaming chat models, {} embedding models",
                chatModels != null ? chatModels.size() : 0,
                streamingChatModels != null ? streamingChatModels.size() : 0,
                embeddingModels != null ? embeddingModels.size() : 0);

        return registry;
    }
}
