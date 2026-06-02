package io.github.afgprojects.framework.ai.spring.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.spring.config.SpringAiProperties;
import io.github.afgprojects.framework.ai.spring.memory.ChatMemoryRepositoryAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Spring AI ChatMemory 自动配置
 *
 * <p>配置 Spring AI 的 {@link MessageChatMemoryAdvisor}，使用 AFG 的
 * {@link ConversationMemory} 作为持久化后端。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(after = SpringAiChatAutoConfiguration.class)
@ConditionalOnClass(ChatMemoryRepository.class)
@ConditionalOnProperty(prefix = "afg.ai.spring", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringAiMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    @ConditionalOnBean(ConversationMemory.class)
    public ChatMemoryRepository chatMemoryRepository(ConversationMemory conversationMemory) {
        log.info("Creating ChatMemoryRepositoryAdapter with ConversationMemory");
        return new ChatMemoryRepositoryAdapter(conversationMemory);
    }

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository inMemoryChatMemoryRepository() {
        log.info("Creating InMemoryChatMemoryRepository");
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    @ConditionalOnBean(ChatMemoryRepository.class)
    @ConditionalOnMissingBean(MessageChatMemoryAdvisor.class)
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(
            ChatMemoryRepository repository,
            SpringAiProperties properties) {
        var chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(20)
            .build();

        log.info("Creating MessageChatMemoryAdvisor");
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
