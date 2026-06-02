package io.github.afgprojects.framework.ai.langchain4j.memory;

import dev.langchain4j.data.message.ChatMessage;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.langchain4j.internal.Lc4jMessageConverter;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 将 AFG ConversationMemory 适配为 LangChain4j ChatMemory
 *
 * <p>LangChain4j 的 {@link dev.langchain4j.memory.ChatMemory} 接口
 * 通过此适配器桥接到 AFG 的 {@link ConversationMemory}，
 * 使得 LangChain4j 的 Agent 和对话流程可以使用 AFG 的记忆存储后端。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class Lc4jChatMemoryAdapter implements dev.langchain4j.memory.ChatMemory {

    private final ConversationMemory conversationMemory;
    private final String sessionId;

    /**
     * 创建适配器
     *
     * @param conversationMemory AFG 对话记忆
     * @param sessionId          会话 ID
     */
    public Lc4jChatMemoryAdapter(@NonNull ConversationMemory conversationMemory,
                                 @NonNull String sessionId) {
        this.conversationMemory = conversationMemory;
        this.sessionId = sessionId;
    }

    @Override
    public void add(@NonNull ChatMessage message) {
        AiMessage aiMessage = Lc4jMessageConverter.fromLc4j(message);
        conversationMemory.addMessage(sessionId, aiMessage);
    }

    @Override
    public void add(@NonNull List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            add(message);
        }
    }

    @NonNull
    @Override
    public List<ChatMessage> messages() {
        List<AiMessage> history = conversationMemory.getHistory(sessionId);
        return Lc4jMessageConverter.toLc4jMessages(history);
    }

    @Override
    public void clear() {
        conversationMemory.clear(sessionId);
    }

    /**
     * 获取会话 ID
     */
    public String sessionId() {
        return sessionId;
    }

    /**
     * 获取底层 AFG ConversationMemory
     */
    public ConversationMemory conversationMemory() {
        return conversationMemory;
    }
}
