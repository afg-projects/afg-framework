package io.github.afgprojects.framework.ai.langchain4j.autoconfigure;

import dev.langchain4j.memory.ChatMemory;
import io.github.afgprojects.framework.ai.core.api.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.langchain4j.config.Lc4jProperties;
import io.github.afgprojects.framework.ai.langchain4j.memory.Lc4jChatMemoryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * LangChain4j ChatMemory 自动配置
 *
 * <p>当 classpath 上存在 LangChain4j {@link ChatMemory} 且
 * {@code afg.ai.langchain4j.enabled=true} 时自动激活。
 * 注册 {@link Lc4jChatMemoryAdapter} 作为 {@link ChatMemory} 的实现，
 * 使用 AFG 的 {@link ConversationMemory} 作为持久化后端。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = Lc4jChatAutoConfiguration.class)
@EnableConfigurationProperties(Lc4jProperties.class)
@ConditionalOnClass(name = "dev.langchain4j.memory.ChatMemory")
@ConditionalOnProperty(prefix = "afg.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Lc4jMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatMemory.class)
    @ConditionalOnBean(ConversationMemory.class)
    public Lc4jChatMemoryAdapter lc4jChatMemoryAdapter(ConversationMemory conversationMemory,
                                                        Lc4jProperties properties) {
        log.info("Creating Lc4jChatMemoryAdapter with ConversationMemory");
        // 使用默认会话 ID，实际使用时可通过 Lc4jChatMemoryAdapter 构造函数指定
        return new Lc4jChatMemoryAdapter(conversationMemory, "default");
    }
}
