package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.langchain4j.chat.Lc4jChatClient;
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
 * LangChain4j ChatClient 自动配置
 *
 * <p>当 classpath 上存在 LangChain4j {@link ChatLanguageModel} 且
 * {@code afg.ai.langchain4j.enabled=true} 时自动激活。
 * 注册 {@link Lc4jChatClient} 作为 {@link AfgChatClient} 的实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.model.chat.ChatLanguageModel")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jChatAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AfgChatClient.class)
    @ConditionalOnBean(ChatLanguageModel.class)
    public Lc4jChatClient lc4jChatClient(ChatLanguageModel chatLanguageModel,
                                          Lc4jProperties properties) {
        log.info("Creating Lc4jChatClient with defaultChatModel={}", properties.getDefaultChatModel());
        return new Lc4jChatClient(chatLanguageModel);
    }

    @Bean
    @ConditionalOnMissingBean(AfgChatClient.class)
    @ConditionalOnBean({ChatLanguageModel.class, StreamingChatLanguageModel.class})
    public Lc4jChatClient lc4jStreamingChatClient(ChatLanguageModel chatLanguageModel,
                                                   StreamingChatLanguageModel streamingChatLanguageModel,
                                                   Lc4jProperties properties) {
        log.info("Creating Lc4jChatClient with streaming support, defaultChatModel={}", properties.getDefaultChatModel());
        return new Lc4jChatClient(chatLanguageModel, streamingChatLanguageModel);
    }
}
