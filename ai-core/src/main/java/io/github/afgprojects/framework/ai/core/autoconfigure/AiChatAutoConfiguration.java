package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
// import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
// import io.github.afgprojects.framework.ai.core.api.chat.EmbeddingClientRegistry;
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

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultChatClientRegistry defaultChatClientRegistry() {
        //     return new DefaultChatClientRegistry();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public DefaultEmbeddingClientRegistry defaultEmbeddingClientRegistry() {
        //     return new DefaultEmbeddingClientRegistry();
        // }

        // TODO: 阶段3添加默认实现Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiChatAspect aiChatAspect() {
        //     return new AiChatAspect();
        // }
    }
}
