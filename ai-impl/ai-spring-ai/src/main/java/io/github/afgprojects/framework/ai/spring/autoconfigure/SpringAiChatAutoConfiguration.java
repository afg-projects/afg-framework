package io.github.afgprojects.framework.ai.spring.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AfgEmbeddingClient;
import io.github.afgprojects.framework.ai.spring.chat.SpringAiChatClient;
import io.github.afgprojects.framework.ai.spring.chat.SpringAiEmbeddingClient;
import io.github.afgprojects.framework.ai.spring.config.SpringAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI ChatClient 自动配置
 *
 * <p>当 classpath 上存在 Spring AI {@link ChatClient} 且 {@code afg.ai.spring.enabled=true} 时自动激活。
 * 注册 {@link SpringAiChatClient} 和 {@link SpringAiEmbeddingClient} 作为
 * {@link AfgChatClient} 和 {@link AfgEmbeddingClient} 的实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(SpringAiProperties.class)
@ConditionalOnClass(name = "org.springframework.ai.chat.client.ChatClient")
@ConditionalOnProperty(prefix = "afg.ai.spring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AfgChatClient.class)
    @ConditionalOnBean(ChatClient.class)
    public SpringAiChatClient springAiChatClient(ChatClient chatClient, SpringAiProperties properties) {
        log.info("Creating SpringAiChatClient with defaultChatModel={}", properties.getDefaultChatModel());
        return new SpringAiChatClient(chatClient);
    }

    @Bean
    @ConditionalOnMissingBean(AfgEmbeddingClient.class)
    @ConditionalOnBean(EmbeddingModel.class)
    public SpringAiEmbeddingClient springAiEmbeddingClient(EmbeddingModel embeddingModel, SpringAiProperties properties) {
        log.info("Creating SpringAiEmbeddingClient with defaultEmbeddingModel={}", properties.getDefaultEmbeddingModel());
        return new SpringAiEmbeddingClient(embeddingModel);
    }
}
