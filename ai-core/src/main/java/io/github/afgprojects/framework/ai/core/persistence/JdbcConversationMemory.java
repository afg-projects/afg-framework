package io.github.afgprojects.framework.ai.core.persistence;

import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.chat.AiRole;
import io.github.afgprojects.framework.ai.core.api.memory.ConversationMemory;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore.Message;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore.MessageRole;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore.MessageStatus;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC 对话记忆实现
 *
 * <p>基于 {@link MessageHistoryStore} 的对话记忆实现，将消息持久化到数据库。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcConversationMemory implements ConversationMemory {

    private final MessageHistoryStore messageHistoryStore;

    @Override
    public void addMessage(@NonNull String sessionId, @NonNull AiMessage message) {
        MessageRole role = convertRole(message.role());
        Map<String, String> metadata = convertMetadata(message.metadata());
        messageHistoryStore.addMessage(sessionId, new MemoryMessage(
                sessionId,
                role,
                message.content() != null ? message.content() : "",
                metadata
        ));
        log.debug("Added message to session {}: role={}", sessionId, role);
    }

    @Override
    @NonNull
    public List<AiMessage> getHistory(@NonNull String sessionId) {
        return messageHistoryStore.getMessages(sessionId).stream()
                .map(this::convertToAiMessage)
                .toList();
    }

    @Override
    public void clear(@NonNull String sessionId) {
        messageHistoryStore.deleteSessionMessages(sessionId);
        log.debug("Cleared conversation memory for session: {}", sessionId);
    }

    @Override
    @NonNull
    public List<AiMessage> getRecentMessages(@NonNull String sessionId, int n) {
        return messageHistoryStore.getRecentMessages(sessionId, n).stream()
                .map(this::convertToAiMessage)
                .toList();
    }

    /**
     * 转换角色枚举：{@link AiRole} -> {@link MessageRole}
     */
    private MessageRole convertRole(AiRole aiRole) {
        return switch (aiRole) {
            case SYSTEM -> MessageRole.SYSTEM;
            case USER -> MessageRole.USER;
            case ASSISTANT -> MessageRole.ASSISTANT;
            case TOOL -> MessageRole.TOOL;
        };
    }

    /**
     * 转换角色枚举：{@link MessageRole} -> {@link AiRole}
     */
    private AiRole convertToAiRole(MessageRole messageRole) {
        return switch (messageRole) {
            case SYSTEM -> AiRole.SYSTEM;
            case USER -> AiRole.USER;
            case ASSISTANT -> AiRole.ASSISTANT;
            case TOOL -> AiRole.TOOL;
        };
    }

    /**
     * 将 {@link AiMessage} 的元数据 {@code Map<String, Object>} 转换为 {@code Map<String, String>}
     */
    private Map<String, String> convertMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>(metadata.size());
        metadata.forEach((key, value) -> result.put(key, value != null ? value.toString() : null));
        return result;
    }

    /**
     * 将存储消息 {@link Message} 转换为 {@link AiMessage}
     */
    @SuppressWarnings("unchecked")
    private AiMessage convertToAiMessage(Message message) {
        Map<String, Object> metadata = (Map<String, Object>) (Map<?, ?>) message.getMetadata();
        return new AiMessage(
                convertToAiRole(message.getRole()),
                message.getContent(),
                List.of(),
                metadata
        );
    }

    /**
     * 对话记忆使用的简单消息实现
     */
    private static class MemoryMessage implements Message {

        private final String sessionId;
        private final MessageRole role;
        private String content;
        private final Instant createdAt;
        private final Map<String, String> metadata;
        private MessageStatus status;

        MemoryMessage(@NonNull String sessionId, @NonNull MessageRole role,
                      @NonNull String content, @NonNull Map<String, String> metadata) {
            this.sessionId = sessionId;
            this.role = role;
            this.content = content;
            this.createdAt = Instant.now();
            this.metadata = metadata;
            this.status = MessageStatus.NORMAL;
        }

        @Override
        @NonNull
        public String getMessageId() {
            return ""; // Assigned by store
        }

        @Override
        @NonNull
        public String getSessionId() {
            return sessionId;
        }

        @Override
        @NonNull
        public MessageRole getRole() {
            return role;
        }

        @Override
        @NonNull
        public String getContent() {
            return content;
        }

        @Override
        public void setContent(@NonNull String content) {
            this.content = content;
        }

        @Override
        @NonNull
        public Instant getCreatedAt() {
            return createdAt;
        }

        @Override
        @Nullable
        public TokenUsage getTokenUsage() {
            return null;
        }

        @Override
        public void setTokenUsage(@Nullable TokenUsage tokenUsage) {
            // No-op
        }

        @Override
        @Nullable
        public String getModelName() {
            return null;
        }

        @Override
        @NonNull
        public Map<String, String> getMetadata() {
            return metadata;
        }

        @Override
        @Nullable
        public String getParentMessageId() {
            return null;
        }

        @Override
        @NonNull
        public MessageStatus getStatus() {
            return status;
        }

        @Override
        public void setStatus(@NonNull MessageStatus status) {
            this.status = status;
        }
    }
}
