package io.github.afgprojects.framework.ai.persistence;

import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 默认消息历史存储实现
 *
 * <p>基于内存的简单消息历史存储，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>单机部署</li>
 *   <li>不需要持久化的场景</li>
 * </ul>
 *
 * <p>生产环境建议使用数据库实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultMessageHistoryStore implements MessageHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(DefaultMessageHistoryStore.class);

    private final ConcurrentHashMap<String, Message> messages = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LinkedList<String>> sessionMessages = new ConcurrentHashMap<>();

    private final int maxMessagesPerSession;

    /**
     * 创建默认消息历史存储
     *
     * @param maxMessagesPerSession 每个会话最大消息数
     */
    public DefaultMessageHistoryStore(int maxMessagesPerSession) {
        this.maxMessagesPerSession = maxMessagesPerSession;
    }

    /**
     * 创建默认消息历史存储（默认每会话最多 1000 条消息）
     */
    public DefaultMessageHistoryStore() {
        this(1000);
    }

    @Override
    @NonNull
    public Message addMessage(@NonNull String sessionId, @NonNull Message message) {
        String messageId = message.getMessageId() != null
                ? message.getMessageId()
                : UUID.randomUUID().toString();

        DefaultMessage newMessage = new DefaultMessage(
                messageId,
                sessionId,
                message.getRole(),
                message.getContent(),
                Instant.now(),
                message.getTokenUsage(),
                message.getModelName(),
                new HashMap<>(message.getMetadata()),
                message.getParentMessageId(),
                MessageStatus.NORMAL
        );

        messages.put(messageId, newMessage);

        // 添加到会话消息列表
        LinkedList<String> sessionMessageList = sessionMessages.computeIfAbsent(sessionId, k -> new LinkedList<>());
        sessionMessageList.add(messageId);

        // 限制会话消息数量
        while (sessionMessageList.size() > maxMessagesPerSession) {
            String oldestMessageId = sessionMessageList.removeFirst();
            messages.remove(oldestMessageId);
            log.debug("Removed oldest message {} from session {}", oldestMessageId, sessionId);
        }

        log.debug("Added message {} to session {}", messageId, sessionId);

        return newMessage;
    }

    @Override
    @NonNull
    public List<Message> getMessages(@NonNull String sessionId) {
        return sessionMessages.getOrDefault(sessionId, new LinkedList<>()).stream()
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<Message> getMessages(@NonNull String sessionId, int offset, int limit) {
        LinkedList<String> messageIds = sessionMessages.getOrDefault(sessionId, new LinkedList<>());

        if (offset >= messageIds.size()) {
            return Collections.emptyList();
        }

        return messageIds.stream()
                .skip(offset)
                .limit(limit)
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<Message> getRecentMessages(@NonNull String sessionId, int limit) {
        LinkedList<String> messageIds = sessionMessages.getOrDefault(sessionId, new LinkedList<>());

        return messageIds.stream()
                .skip(Math.max(0, messageIds.size() - limit))
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .collect(Collectors.toList());
    }

    @Override
    @Nullable
    public Message getMessage(@NonNull String messageId) {
        return messages.get(messageId);
    }

    @Override
    public void updateMessage(@NonNull Message message) {
        if (messages.containsKey(message.getMessageId())) {
            messages.put(message.getMessageId(), message);
            log.debug("Updated message {}", message.getMessageId());
        }
    }

    @Override
    public void deleteMessage(@NonNull String messageId) {
        Message message = messages.get(messageId);
        if (message != null) {
            message.setStatus(MessageStatus.DELETED);
            log.debug("Deleted message {}", messageId);
        }
    }

    @Override
    public void deleteSessionMessages(@NonNull String sessionId) {
        LinkedList<String> messageIds = sessionMessages.remove(sessionId);
        if (messageIds != null) {
            messageIds.forEach(messages::remove);
        }
        log.info("Deleted all messages for session {}", sessionId);
    }

    @Override
    @NonNull
    public List<Message> searchMessages(@NonNull String sessionId, @NonNull String query, int limit) {
        String lowerQuery = query.toLowerCase();

        return sessionMessages.getOrDefault(sessionId, new LinkedList<>()).stream()
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .filter(m -> m.getContent().toLowerCase().contains(lowerQuery))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public long getMessageCount(@NonNull String sessionId) {
        return sessionMessages.getOrDefault(sessionId, new LinkedList<>()).stream()
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .count();
    }

    @Override
    @NonNull
    public TokenStats getTokenStats(@NonNull String sessionId) {
        AtomicLong totalInputTokens = new AtomicLong(0);
        AtomicLong totalOutputTokens = new AtomicLong(0);
        AtomicLong userMessageTokens = new AtomicLong(0);
        AtomicLong assistantMessageTokens = new AtomicLong(0);
        AtomicLong messageCount = new AtomicLong(0);

        sessionMessages.getOrDefault(sessionId, new LinkedList<>()).stream()
                .map(messages::get)
                .filter(Objects::nonNull)
                .filter(m -> m.getStatus() != MessageStatus.DELETED)
                .filter(m -> m.getTokenUsage() != null)
                .forEach(m -> {
                    TokenUsage usage = m.getTokenUsage();
                    totalInputTokens.addAndGet(usage.getInputTokens());
                    totalOutputTokens.addAndGet(usage.getOutputTokens());
                    messageCount.incrementAndGet();

                    if (m.getRole() == MessageRole.USER) {
                        userMessageTokens.addAndGet(usage.getTotalTokens());
                    } else if (m.getRole() == MessageRole.ASSISTANT) {
                        assistantMessageTokens.addAndGet(usage.getTotalTokens());
                    }
                });

        return new DefaultTokenStats(
                totalInputTokens.get(),
                totalOutputTokens.get(),
                userMessageTokens.get(),
                assistantMessageTokens.get(),
                messageCount.get()
        );
    }

    /**
     * 默认消息实现
     */
    private static class DefaultMessage implements Message {
        private final String messageId;
        private final String sessionId;
        private final MessageRole role;
        private volatile String content;
        private final Instant createdAt;
        private volatile TokenUsage tokenUsage;
        private final String modelName;
        private final Map<String, String> metadata;
        private final String parentMessageId;
        private volatile MessageStatus status;

        DefaultMessage(String messageId, String sessionId, MessageRole role, String content,
                       Instant createdAt, TokenUsage tokenUsage, String modelName,
                       Map<String, String> metadata, String parentMessageId, MessageStatus status) {
            this.messageId = messageId;
            this.sessionId = sessionId;
            this.role = role;
            this.content = content;
            this.createdAt = createdAt;
            this.tokenUsage = tokenUsage;
            this.modelName = modelName;
            this.metadata = metadata;
            this.parentMessageId = parentMessageId;
            this.status = status;
        }

        @Override
        @NonNull
        public String getMessageId() { return messageId; }

        @Override
        @NonNull
        public String getSessionId() { return sessionId; }

        @Override
        @NonNull
        public MessageRole getRole() { return role; }

        @Override
        @NonNull
        public String getContent() { return content; }

        @Override
        public void setContent(@NonNull String newContent) { this.content = newContent; }

        @Override
        @NonNull
        public Instant getCreatedAt() { return createdAt; }

        @Override
        @Nullable
        public TokenUsage getTokenUsage() { return tokenUsage; }

        @Override
        public void setTokenUsage(@Nullable TokenUsage usage) { this.tokenUsage = usage; }

        @Override
        @Nullable
        public String getModelName() { return modelName; }

        @Override
        @NonNull
        public Map<String, String> getMetadata() { return metadata; }

        @Override
        @Nullable
        public String getParentMessageId() { return parentMessageId; }

        @Override
        @NonNull
        public MessageStatus getStatus() { return status; }

        @Override
        public void setStatus(@NonNull MessageStatus newStatus) { this.status = newStatus; }
    }

    /**
     * 默认 Token 使用量实现
     */
    private static class DefaultTokenUsage implements TokenUsage {
        private final long inputTokens;
        private final long outputTokens;

        DefaultTokenUsage(long inputTokens, long outputTokens) {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
        }

        @Override
        public long getInputTokens() { return inputTokens; }

        @Override
        public long getOutputTokens() { return outputTokens; }

        @Override
        public long getTotalTokens() { return inputTokens + outputTokens; }
    }

    /**
     * 默认 Token 统计实现
     */
    private static class DefaultTokenStats implements TokenStats {
        private final long totalInputTokens;
        private final long totalOutputTokens;
        private final long userMessageTokens;
        private final long assistantMessageTokens;
        private final long messageCount;

        DefaultTokenStats(long totalInputTokens, long totalOutputTokens,
                          long userMessageTokens, long assistantMessageTokens, long messageCount) {
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.userMessageTokens = userMessageTokens;
            this.assistantMessageTokens = assistantMessageTokens;
            this.messageCount = messageCount;
        }

        @Override
        public long getTotalInputTokens() { return totalInputTokens; }

        @Override
        public long getTotalOutputTokens() { return totalOutputTokens; }

        @Override
        public long getTotalTokens() { return totalInputTokens + totalOutputTokens; }

        @Override
        public long getUserMessageTokens() { return userMessageTokens; }

        @Override
        public long getAssistantMessageTokens() { return assistantMessageTokens; }

        @Override
        public double getAverageTokensPerMessage() {
            return messageCount > 0 ? getTotalTokens() / (double) messageCount : 0.0;
        }
    }
}