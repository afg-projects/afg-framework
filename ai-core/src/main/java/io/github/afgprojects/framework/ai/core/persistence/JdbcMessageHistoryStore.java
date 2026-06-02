package io.github.afgprojects.framework.ai.core.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore.*;
import io.github.afgprojects.framework.data.core.DataManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * JDBC 消息历史存储实现（基于 DataManager）
 *
 * <p>使用 DataManager 进行数据库操作，适用于生产环境。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class JdbcMessageHistoryStore implements MessageHistoryStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcMessageHistoryStore.class);

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public JdbcMessageHistoryStore(@NonNull DataManager dataManager, @NonNull String tableName) {
        this.dataManager = dataManager;
        this.objectMapper = new ObjectMapper();
        this.tableName = tableName;
        log.info("JdbcMessageHistoryStore initialized: table={}", tableName);
    }

    public JdbcMessageHistoryStore(@NonNull DataManager dataManager) {
        this(dataManager, "ai_message");
    }

    @Override
    @NonNull
    public Message addMessage(@NonNull String sessionId, @NonNull Message message) {
        String messageId = message.getMessageId() != null ? message.getMessageId() : UUID.randomUUID().toString();
        Instant now = Instant.now();

        String sql = """
            INSERT INTO %s (message_id, session_id, role, content, created_at, input_tokens, output_tokens,
                           model_name, metadata, parent_message_id, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """.formatted(tableName);

        TokenUsage usage = message.getTokenUsage();
        dataManager.executeUpdate(sql, List.of(
                messageId,
                sessionId,
                message.getRole().name(),
                message.getContent(),
                java.sql.Timestamp.from(now),
                usage != null ? usage.getInputTokens() : null,
                usage != null ? usage.getOutputTokens() : null,
                message.getModelName(),
                toJson(message.getMetadata()),
                message.getParentMessageId(),
                MessageStatus.NORMAL.name()
        ));

        log.debug("Added message {} to session {}", messageId, sessionId);

        return new JdbcMessage(messageId, sessionId, message.getRole(), message.getContent(),
                now, usage, message.getModelName(), new HashMap<>(message.getMetadata()),
                message.getParentMessageId(), MessageStatus.NORMAL);
    }

    @Override
    @NonNull
    public List<Message> getMessages(@NonNull String sessionId) {
        String sql = "SELECT * FROM %s WHERE session_id = ? ORDER BY created_at".formatted(tableName);
        return dataManager.queryForList(sql, List.of(sessionId), this::mapRow).stream().map(m -> (Message) m).toList();
    }

    @Override
    @NonNull
    public List<Message> getMessages(@NonNull String sessionId, int offset, int limit) {
        String sql = "SELECT * FROM %s WHERE session_id = ? ORDER BY created_at OFFSET ? LIMIT ?".formatted(tableName);
        return dataManager.queryForList(sql, List.of(sessionId, offset, limit), this::mapRow).stream().map(m -> (Message) m).toList();
    }

    @Override
    @NonNull
    public List<Message> getRecentMessages(@NonNull String sessionId, int limit) {
        String sql = """
            SELECT * FROM %s WHERE session_id = ?
            ORDER BY created_at DESC LIMIT ?
            """.formatted(tableName);
        List<Message> messages = new ArrayList<>(dataManager.queryForList(sql, List.of(sessionId, limit), this::mapRow)
                .stream().map(m -> (Message) m).toList());
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public @Nullable Message getMessage(@NonNull String messageId) {
        String sql = "SELECT * FROM %s WHERE message_id = ?".formatted(tableName);
        List<JdbcMessage> messages = dataManager.queryForList(sql, List.of(messageId), this::mapRow);
        return messages.isEmpty() ? null : messages.get(0);
    }

    @Override
    public void updateMessage(@NonNull Message message) {
        String sql = """
            UPDATE %s SET content = ?, input_tokens = ?, output_tokens = ?, status = ?, metadata = ?::jsonb
            WHERE message_id = ?
            """.formatted(tableName);

        TokenUsage usage = message.getTokenUsage();
        dataManager.executeUpdate(sql, List.of(
                message.getContent(),
                usage != null ? usage.getInputTokens() : null,
                usage != null ? usage.getOutputTokens() : null,
                message.getStatus().name(),
                toJson(message.getMetadata()),
                message.getMessageId()
        ));

        log.debug("Updated message {}", message.getMessageId());
    }

    @Override
    public void deleteMessage(@NonNull String messageId) {
        String sql = "UPDATE %s SET status = ? WHERE message_id = ?".formatted(tableName);
        dataManager.executeUpdate(sql, List.of(MessageStatus.DELETED.name(), messageId));
        log.debug("Deleted message {}", messageId);
    }

    @Override
    public void deleteSessionMessages(@NonNull String sessionId) {
        String sql = "DELETE FROM %s WHERE session_id = ?".formatted(tableName);
        dataManager.executeUpdate(sql, List.of(sessionId));
        log.info("Deleted all messages for session {}", sessionId);
    }

    @Override
    @NonNull
    public List<Message> searchMessages(@NonNull String sessionId, @NonNull String query, int limit) {
        String sql = """
            SELECT * FROM %s WHERE session_id = ? AND content ILIKE ?
            AND status != ? ORDER BY created_at DESC LIMIT ?
            """.formatted(tableName);

        return dataManager.queryForList(sql, List.of(sessionId, "%" + query + "%", MessageStatus.DELETED.name(), limit), this::mapRow)
                .stream().map(m -> (Message) m).toList();
    }

    @Override
    public long getMessageCount(@NonNull String sessionId) {
        String sql = "SELECT COUNT(*) FROM %s WHERE session_id = ? AND status != ?".formatted(tableName);
        return dataManager.queryForCount(sql, List.of(sessionId, MessageStatus.DELETED.name()));
    }

    @Override
    @NonNull
    public TokenStats getTokenStats(@NonNull String sessionId) {
        String sql = """
            SELECT
                COALESCE(SUM(input_tokens), 0),
                COALESCE(SUM(output_tokens), 0),
                COALESCE(SUM(CASE WHEN role = 'USER' THEN COALESCE(input_tokens, 0) + COALESCE(output_tokens, 0) ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN role = 'ASSISTANT' THEN COALESCE(input_tokens, 0) + COALESCE(output_tokens, 0) ELSE 0 END), 0),
                COUNT(*)
            FROM %s WHERE session_id = ? AND status != ?
            """.formatted(tableName);

        return dataManager.queryForObject(sql, List.of(sessionId, MessageStatus.DELETED.name()),
                (rs, rowNum) -> new DefaultTokenStats(
                        rs.getLong(1),
                        rs.getLong(2),
                        rs.getLong(3),
                        rs.getLong(4),
                        rs.getLong(5)
                ));
    }

    private JdbcMessage mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        long inputTokensValue = rs.getLong("input_tokens");
        boolean inputNull = rs.wasNull();
        long outputTokensValue = rs.getLong("output_tokens");
        boolean outputNull = rs.wasNull();

        TokenUsage usage = (inputNull && outputNull) ? null
                : new DefaultTokenUsage(inputNull ? 0L : inputTokensValue, outputNull ? 0L : outputTokensValue);

        return new JdbcMessage(
                rs.getString("message_id"),
                rs.getString("session_id"),
                MessageRole.valueOf(rs.getString("role")),
                rs.getString("content"),
                rs.getTimestamp("created_at").toInstant(),
                usage,
                rs.getString("model_name"),
                parseJson(rs.getString("metadata")),
                rs.getString("parent_message_id"),
                MessageStatus.valueOf(rs.getString("status"))
        );
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private class JdbcMessage implements Message {
        private final String messageId;
        private final String sessionId;
        private final MessageRole role;
        private String content;
        private final Instant createdAt;
        private TokenUsage tokenUsage;
        private final String modelName;
        private final Map<String, String> metadata;
        private final String parentMessageId;
        private MessageStatus status;

        JdbcMessage(String messageId, String sessionId, MessageRole role, String content,
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

        @Override @NonNull public String getMessageId() { return messageId; }
        @Override @NonNull public String getSessionId() { return sessionId; }
        @Override @NonNull public MessageRole getRole() { return role; }
        @Override @NonNull public String getContent() { return content; }
        @Override public void setContent(@NonNull String c) { this.content = c; }
        @Override @NonNull public Instant getCreatedAt() { return createdAt; }
        @Override @Nullable public TokenUsage getTokenUsage() { return tokenUsage; }
        @Override public void setTokenUsage(@Nullable TokenUsage u) { this.tokenUsage = u; }
        @Override @Nullable public String getModelName() { return modelName; }
        @Override @NonNull public Map<String, String> getMetadata() { return metadata; }
        @Override @Nullable public String getParentMessageId() { return parentMessageId; }
        @Override @NonNull public MessageStatus getStatus() { return status; }
        @Override public void setStatus(@NonNull MessageStatus s) { this.status = s; }
    }

    private record DefaultTokenUsage(long inputTokens, long outputTokens) implements TokenUsage {
        @Override public long getInputTokens() { return inputTokens; }
        @Override public long getOutputTokens() { return outputTokens; }
        @Override public long getTotalTokens() { return inputTokens + outputTokens; }
    }

    private record DefaultTokenStats(long totalInputTokens, long totalOutputTokens,
                                      long userMessageTokens, long assistantMessageTokens,
                                      long messageCount) implements TokenStats {
        @Override public long getTotalInputTokens() { return totalInputTokens; }
        @Override public long getTotalOutputTokens() { return totalOutputTokens; }
        @Override public long getTotalTokens() { return totalInputTokens + totalOutputTokens; }
        @Override public long getUserMessageTokens() { return userMessageTokens; }
        @Override public long getAssistantMessageTokens() { return assistantMessageTokens; }
        @Override public double getAverageTokensPerMessage() {
            return messageCount > 0 ? getTotalTokens() / (double) messageCount : 0.0;
        }
    }
}
