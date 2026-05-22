package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.memory.Message;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 AFG ConversationMemory 适配为 Spring AI ChatMemory
 *
 * <p>使得 AFG 的会话存储可以在 Spring AI 的 MessageChatMemoryAdvisor 中使用。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ChatMemoryAdapter implements org.springframework.ai.chat.memory.ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryAdapter.class);

    private final ConversationMemory delegate;

    public ChatMemoryAdapter(@NonNull ConversationMemory delegate) {
        this.delegate = delegate;
    }

    @Override
    public void add(@NonNull String conversationId, @NonNull List<org.springframework.ai.chat.messages.Message> messages) {
        log.debug("ChatMemoryAdapter.add: conversationId={}, count={}", conversationId, messages.size());
        for (org.springframework.ai.chat.messages.Message springAiMessage : messages) {
            Message afgMessage = convertToAfgMessage(springAiMessage);
            if (afgMessage != null) {
                delegate.addMessage(conversationId, afgMessage);
            }
        }
    }

    @Override
    @NonNull
    public List<org.springframework.ai.chat.messages.Message> get(@NonNull String conversationId) {
        log.debug("ChatMemoryAdapter.get: conversationId={}", conversationId);
        List<Message> afgMessages = delegate.getHistory(conversationId);

        List<org.springframework.ai.chat.messages.Message> result = new ArrayList<>();
        for (Message afgMessage : afgMessages) {
            org.springframework.ai.chat.messages.Message springAiMessage = convertToSpringAiMessage(afgMessage);
            if (springAiMessage != null) {
                result.add(springAiMessage);
            }
        }
        return result;
    }

    @Override
    public void clear(@NonNull String conversationId) {
        log.debug("ChatMemoryAdapter.clear: conversationId={}", conversationId);
        delegate.clear(conversationId);
    }

    private Message convertToAfgMessage(org.springframework.ai.chat.messages.Message springAiMessage) {
        if (springAiMessage instanceof org.springframework.ai.chat.messages.UserMessage userMsg) {
            return Message.user(userMsg.getText());
        } else if (springAiMessage instanceof org.springframework.ai.chat.messages.AssistantMessage assistantMsg) {
            return Message.assistant(assistantMsg.getText());
        } else if (springAiMessage instanceof org.springframework.ai.chat.messages.SystemMessage systemMsg) {
            return Message.system(systemMsg.getText());
        }
        return null;
    }

    private org.springframework.ai.chat.messages.Message convertToSpringAiMessage(Message afgMessage) {
        return switch (afgMessage.role()) {
            case USER -> new org.springframework.ai.chat.messages.UserMessage(afgMessage.content());
            case ASSISTANT -> new org.springframework.ai.chat.messages.AssistantMessage(afgMessage.content());
            case SYSTEM -> new org.springframework.ai.chat.messages.SystemMessage(afgMessage.content());
            case TOOL -> new org.springframework.ai.chat.messages.AssistantMessage(afgMessage.content());
        };
    }
}
