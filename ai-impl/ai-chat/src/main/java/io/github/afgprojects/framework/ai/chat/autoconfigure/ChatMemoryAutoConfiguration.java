package io.github.afgprojects.framework.ai.chat.autoconfigure;

import io.github.afgprojects.framework.ai.chat.memory.ChatMemoryRepositoryAdapter;
import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.persistence.MessageHistoryStore;
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
 * ChatMemory 自动配置
 *
 * <p>配置 Spring AI 的 {@link MessageChatMemoryAdvisor}，使用 AFG 的
 * {@link ConversationMemory} 作为持久化后端。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = ChatClientAutoConfiguration.class)
@ConditionalOnClass(ChatMemoryRepository.class)
@ConditionalOnProperty(prefix = "afg.ai.chat.memory", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatMemoryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    @ConditionalOnBean(ConversationMemory.class)
    public ChatMemoryRepository chatMemoryRepository(ConversationMemory conversationMemory) {
        return new ChatMemoryRepositoryAdapter(conversationMemory);
    }

    @Bean
    @ConditionalOnMissingBean(ChatMemoryRepository.class)
    public ChatMemoryRepository inMemoryChatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    @Bean
    @ConditionalOnBean(ChatMemoryRepository.class)
    @ConditionalOnMissingBean(MessageChatMemoryAdvisor.class)
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(
            ChatMemoryRepository repository,
            ChatClientProperties properties) {
        var chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(repository)
            .maxMessages(properties.getMemory().getMaxMessages())
            .build();

        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }
}
