package io.github.afgprojects.framework.ai.core.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.persistence.*;
import io.github.afgprojects.framework.data.core.DataManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * JDBC 会话存储实现（基于 DataManager）
 *
 * <p>使用 DataManager 进行数据库操作，适用于生产环境。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class JdbcSessionStore implements SessionStore {

    private static final Logger log = LoggerFactory.getLogger(JdbcSessionStore.class);

    private final DataManager dataManager;
    private final ObjectMapper objectMapper;
    private final String tableName;

    public JdbcSessionStore(@NonNull DataManager dataManager, @NonNull String tableName) {
        this.dataManager = dataManager;
        this.objectMapper = new ObjectMapper();
        this.tableName = tableName;
        log.info("JdbcSessionStore initialized: table={}", tableName);
    }

    public JdbcSessionStore(@NonNull DataManager dataManager) {
        this(dataManager, "ai_session");
    }

    @Override
    @NonNull
    public Session createSession(@NonNull SessionContext context) {
        return createSession(UUID.randomUUID().toString(), context);
    }

    @Override
    @NonNull
    public Session createSession(@NonNull String sessionId, @NonNull SessionContext context) {
        Instant now = Instant.now();
        Instant expiresAt = context.getExpiresInSeconds() != null
                ? now.plusSeconds(context.getExpiresInSeconds())
                : null;

        String metadataJson = toJson(context.getMetadata());

        String sql = """
            INSERT INTO %s (session_id, user_id, tenant_id, state, created_at, last_active_at,
                           expires_at, model_name, system_prompt, metadata, message_count, total_tokens)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, 0, 0)
            """.formatted(tableName);

        dataManager.executeUpdate(sql, List.of(
                sessionId,
                context.getUserId(),
                context.getTenantId(),
                SessionState.ACTIVE.name(),
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now),
                expiresAt != null ? java.sql.Timestamp.from(expiresAt) : null,
                context.getModelName(),
                context.getSystemPrompt(),
                metadataJson
        ));

        log.info("Created session {} for user {}", sessionId, context.getUserId());

        return new JdbcSession(sessionId, context.getUserId(), context.getTenantId(),
                SessionState.ACTIVE, now, now, expiresAt, context.getModelName(),
                context.getSystemPrompt(), new HashMap<>(context.getMetadata()), 0, 0L);
    }

    @Override
    public @Nullable Session getSession(@NonNull String sessionId) {
        String sql = "SELECT * FROM %s WHERE session_id = ?".formatted(tableName);

        List<JdbcSession> sessions = dataManager.queryForList(sql, List.of(sessionId), (rs, rowNum) -> new JdbcSession(
                rs.getString("session_id"),
                rs.getString("user_id"),
                rs.getString("tenant_id"),
                SessionState.valueOf(rs.getString("state")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_active_at").toInstant(),
                rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
                rs.getString("model_name"),
                rs.getString("system_prompt"),
                parseJson(rs.getString("metadata")),
                rs.getInt("message_count"),
                rs.getLong("total_tokens")
        ));

        return sessions.isEmpty() ? null : sessions.get(0);
    }

    @Override
    public void updateSession(@NonNull Session session) {
        String sql = """
            UPDATE %s SET user_id = ?, tenant_id = ?, state = ?, last_active_at = ?,
                           expires_at = ?, model_name = ?, system_prompt = ?,
                           message_count = ?, total_tokens = ?, metadata = ?::jsonb
            WHERE session_id = ?
            """.formatted(tableName);

        dataManager.executeUpdate(sql, List.of(
                session.getUserId(),
                session.getTenantId(),
                session.getState().name(),
                java.sql.Timestamp.from(session.getLastActiveAt()),
                session.getExpiresAt() != null ? java.sql.Timestamp.from(session.getExpiresAt()) : null,
                session.getModelName(),
                session.getSystemPrompt(),
                session.getMessageCount(),
                session.getTotalTokens(),
                toJson(session.getMetadata()),
                session.getSessionId()
        ));

        log.debug("Updated session {}", session.getSessionId());
    }

    @Override
    public void deleteSession(@NonNull String sessionId) {
        String sql = "DELETE FROM %s WHERE session_id = ?".formatted(tableName);
        dataManager.executeUpdate(sql, List.of(sessionId));
        log.info("Deleted session {}", sessionId);
    }

    @Override
    @NonNull
    public List<Session> getActiveSessions(@NonNull String userId, int limit) {
        String sql = """
            SELECT * FROM %s WHERE user_id = ? AND state = ?
            AND (expires_at IS NULL OR expires_at > NOW())
            ORDER BY last_active_at DESC LIMIT ?
            """.formatted(tableName);

        return dataManager.queryForList(sql, List.of(userId, SessionState.ACTIVE.name(), limit), (rs, rowNum) -> new JdbcSession(
                rs.getString("session_id"),
                rs.getString("user_id"),
                rs.getString("tenant_id"),
                SessionState.valueOf(rs.getString("state")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_active_at").toInstant(),
                rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
                rs.getString("model_name"),
                rs.getString("system_prompt"),
                parseJson(rs.getString("metadata")),
                rs.getInt("message_count"),
                rs.getLong("total_tokens")
        )).stream().map(s -> (Session) s).toList();
    }

    @NonNull
    public List<Session> getAllActiveSessions(int limit) {
        String sql = """
            SELECT * FROM %s WHERE state = ?
            AND (expires_at IS NULL OR expires_at > NOW())
            ORDER BY last_active_at DESC LIMIT ?
            """.formatted(tableName);

        return dataManager.queryForList(sql, List.of(SessionState.ACTIVE.name(), limit), (rs, rowNum) -> new JdbcSession(
                rs.getString("session_id"),
                rs.getString("user_id"),
                rs.getString("tenant_id"),
                SessionState.valueOf(rs.getString("state")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("last_active_at").toInstant(),
                rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
                rs.getString("model_name"),
                rs.getString("system_prompt"),
                parseJson(rs.getString("metadata")),
                rs.getInt("message_count"),
                rs.getLong("total_tokens")
        )).stream().map(s -> (Session) s).toList();
    }

    @Override
    @NonNull
    public List<Session> getSessionHistory(@NonNull String userId, @Nullable Instant start, @Nullable Instant end, int limit) {
        String sql = """
            SELECT * FROM %s WHERE user_id = ?
            AND (? IS NULL OR created_at >= ?)
            AND (? IS NULL OR created_at <= ?)
            ORDER BY created_at DESC LIMIT ?
            """.formatted(tableName);

        return dataManager.queryForList(sql, List.of(
                        userId,
                        start != null ? java.sql.Timestamp.from(start) : null,
                        start != null ? java.sql.Timestamp.from(start) : null,
                        end != null ? java.sql.Timestamp.from(end) : null,
                        end != null ? java.sql.Timestamp.from(end) : null,
                        limit),
                (rs, rowNum) -> new JdbcSession(
                        rs.getString("session_id"),
                        rs.getString("user_id"),
                        rs.getString("tenant_id"),
                        SessionState.valueOf(rs.getString("state")),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("last_active_at").toInstant(),
                        rs.getTimestamp("expires_at") != null ? rs.getTimestamp("expires_at").toInstant() : null,
                        rs.getString("model_name"),
                        rs.getString("system_prompt"),
                        parseJson(rs.getString("metadata")),
                        rs.getInt("message_count"),
                        rs.getLong("total_tokens")
                ))
                .stream().map(s -> (Session) s).toList();
    }

    @Override
    public int cleanupExpiredSessions() {
        String sql = "UPDATE %s SET state = ? WHERE expires_at < NOW() AND state != ?".formatted(tableName);
        int count = dataManager.executeUpdate(sql, List.of(SessionState.EXPIRED.name(), SessionState.EXPIRED.name()));
        log.info("Cleaned up {} expired sessions", count);
        return count;
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.warn("Failed to serialize metadata", e);
            return "{}";
        }
    }

    private Map<String, String> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private class JdbcSession implements Session {
        private final String sessionId;
        private final String userId;
        private final String tenantId;
        private SessionState state;
        private final Instant createdAt;
        private Instant lastActiveAt;
        private final Instant expiresAt;
        private final String modelName;
        private final String systemPrompt;
        private final Map<String, String> metadata;
        private int messageCount;
        private long totalTokens;

        JdbcSession(String sessionId, String userId, String tenantId, SessionState state,
                    Instant createdAt, Instant lastActiveAt, Instant expiresAt,
                    String modelName, String systemPrompt, Map<String, String> metadata,
                    int messageCount, long totalTokens) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.tenantId = tenantId;
            this.state = state;
            this.createdAt = createdAt;
            this.lastActiveAt = lastActiveAt;
            this.expiresAt = expiresAt;
            this.modelName = modelName;
            this.systemPrompt = systemPrompt;
            this.metadata = metadata;
            this.messageCount = messageCount;
            this.totalTokens = totalTokens;
        }

        @Override @NonNull public String getSessionId() { return sessionId; }
        @Override @Nullable public String getUserId() { return userId; }
        @Override @Nullable public String getTenantId() { return tenantId; }
        @Override @NonNull public SessionState getState() { return state; }
        @Override public void setState(@NonNull SessionState s) { this.state = s; }
        @Override @NonNull public Instant getCreatedAt() { return createdAt; }
        @Override @NonNull public Instant getLastActiveAt() { return lastActiveAt; }
        @Override public void updateLastActiveAt() { this.lastActiveAt = Instant.now(); }
        @Override @Nullable public Instant getExpiresAt() { return expiresAt; }
        @Override @Nullable public String getModelName() { return modelName; }
        @Override @Nullable public String getSystemPrompt() { return systemPrompt; }
        @Override @NonNull public Map<String, String> getMetadata() { return metadata; }
        @Override public void setMetadata(@NonNull String key, @NonNull String value) { metadata.put(key, value); }
        @Override public int getMessageCount() { return messageCount; }
        @Override public long getTotalTokens() { return totalTokens; }
        @Override public boolean isExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }

        public void incrementMessageCount() { this.messageCount++; }
        public void addTokens(long tokens) { this.totalTokens += tokens; }
    }
}
