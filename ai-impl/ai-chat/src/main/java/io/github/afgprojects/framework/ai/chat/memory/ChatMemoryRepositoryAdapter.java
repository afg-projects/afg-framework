package io.github.afgprojects.framework.ai.chat.memory;

import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * AFG ConversationMemory → Spring AI ChatMemoryRepository 适配器
 *
 * <p>将 AFG 框架的 {@link ConversationMemory} 适配为 Spring AI 1.1.6 的
 * {@link ChatMemoryRepository} 接口，使 Spring AI 的 {@code MessageChatMemoryAdvisor}
 * 可以直接使用 AFG 的持久化层。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class ChatMemoryRepositoryAdapter implements ChatMemoryRepository {

    private final ConversationMemory conversationMemory;

    @Override
    @NonNull
    public List<String> findConversationIds() {
        // ConversationMemory 没有列出所有会话 ID 的方法，返回空列表
        return List.of();
    }

    @Override
    @NonNull
    public List<Message> findByConversationId(@NonNull String conversationId) {
        var history = conversationMemory.getHistory(conversationId);
        return AiMessageConverter.toSpringAiMessages(history);
    }

    @Override
    public void saveAll(@NonNull String conversationId, @NonNull List<Message> messages) {
        // 先清除旧消息再保存新消息（Spring AI 的 saveAll 是全量替换语义）
        conversationMemory.clear(conversationId);
        for (Message message : messages) {
            var aiMessage = AiMessageConverter.fromSpringAi(message);
            conversationMemory.addMessage(conversationId, aiMessage);
        }
    }

    @Override
    public void deleteByConversationId(@NonNull String conversationId) {
        conversationMemory.clear(conversationId);
    }
}