package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.chat.DefaultChatClientRegistry;
import io.github.afgprojects.framework.ai.core.chat.DefaultEmbeddingClientRegistry;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.chat.EmbeddingClientRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AFG AI 对话客户端自动配置。
 *
 * <p>配置前缀：{@code afg.ai.chat}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiChatAutoConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.chat", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class ChatConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ChatClientRegistry defaultChatClientRegistry() {
            return new DefaultChatClientRegistry();
        }

        @Bean
        @ConditionalOnMissingBean
        public EmbeddingClientRegistry defaultEmbeddingClientRegistry() {
            return new DefaultEmbeddingClientRegistry();
        }
    }
}
