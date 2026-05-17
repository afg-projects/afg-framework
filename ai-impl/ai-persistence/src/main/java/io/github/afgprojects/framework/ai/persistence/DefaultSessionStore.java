package io.github.afgprojects.framework.ai.persistence;

import io.github.afgprojects.framework.ai.core.persistence.SessionStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认会话存储实现
 *
 * <p>基于内存的简单会话存储，适用于：
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
public class DefaultSessionStore implements SessionStore {

    private static final Logger log = LoggerFactory.getLogger(DefaultSessionStore.class);

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<String>> userSessions = new ConcurrentHashMap<>();

    private final int maxSessionsPerUser;

    /**
     * 创建默认会话存储
     *
     * @param maxSessionsPerUser 每个用户最大会话数
     */
    public DefaultSessionStore(int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    /**
     * 创建默认会话存储（默认每用户最多 100 个会话）
     */
    public DefaultSessionStore() {
        this(100);
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

        DefaultSession session = new DefaultSession(
                sessionId,
                context.getUserId(),
                context.getTenantId(),
                SessionState.ACTIVE,
                now,
                now,
                expiresAt,
                context.getModelName(),
                context.getSystemPrompt(),
                new HashMap<>(context.getMetadata()),
                0,
                0L
        );

        sessions.put(sessionId, session);

        // 添加到用户会话列表
        if (context.getUserId() != null) {
            userSessions.computeIfAbsent(context.getUserId(), k -> new ArrayList<>())
                    .add(0, sessionId);

            // 限制用户会话数量
            List<String> userSessionList = userSessions.get(context.getUserId());
            while (userSessionList.size() > maxSessionsPerUser) {
                String oldestSessionId = userSessionList.remove(userSessionList.size() - 1);
                sessions.remove(oldestSessionId);
                log.debug("Removed oldest session {} for user {}", oldestSessionId, context.getUserId());
            }
        }

        log.info("Created session {} for user {}", sessionId, context.getUserId());

        return session;
    }

    @Override
    @Nullable
    public Session getSession(@NonNull String sessionId) {
        Session session = sessions.get(sessionId);

        if (session != null && session.isExpired()) {
            session.setState(SessionState.EXPIRED);
            return session;
        }

        return session;
    }

    @Override
    public void updateSession(@NonNull Session session) {
        if (sessions.containsKey(session.getSessionId())) {
            sessions.put(session.getSessionId(), session);
            log.debug("Updated session {}", session.getSessionId());
        }
    }

    @Override
    public void deleteSession(@NonNull String sessionId) {
        Session session = sessions.remove(sessionId);

        if (session != null && session.getUserId() != null) {
            userSessions.getOrDefault(session.getUserId(), Collections.emptyList())
                    .remove(sessionId);
        }

        log.info("Deleted session {}", sessionId);
    }

    @Override
    @NonNull
    public List<Session> getActiveSessions(@NonNull String userId, int limit) {
        return userSessions.getOrDefault(userId, Collections.emptyList()).stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .filter(s -> s.getState() == SessionState.ACTIVE)
                .filter(s -> !s.isExpired())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @NonNull
    public List<Session> getSessionHistory(@NonNull String userId, @Nullable Instant start, @Nullable Instant end, int limit) {
        return userSessions.getOrDefault(userId, Collections.emptyList()).stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .filter(s -> start == null || s.getCreatedAt().isAfter(start))
                .filter(s -> end == null || s.getCreatedAt().isBefore(end))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public int cleanupExpiredSessions() {
        int count = 0;

        Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> entry = iterator.next();
            Session session = entry.getValue();

            if (session.isExpired() && session.getState() != SessionState.EXPIRED) {
                session.setState(SessionState.EXPIRED);
                count++;
                log.debug("Marked session {} as expired", entry.getKey());
            }
        }

        log.info("Cleaned up {} expired sessions", count);
        return count;
    }

    /**
     * 默认会话实现
     */
    private static class DefaultSession implements Session {
        private final String sessionId;
        private final String userId;
        private final String tenantId;
        private volatile SessionState state;
        private final Instant createdAt;
        private volatile Instant lastActiveAt;
        private final Instant expiresAt;
        private final String modelName;
        private final String systemPrompt;
        private final Map<String, String> metadata;
        private volatile int messageCount;
        private volatile long totalTokens;

        DefaultSession(String sessionId, String userId, String tenantId, SessionState state,
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

        @Override
        @NonNull
        public String getSessionId() { return sessionId; }

        @Override
        @Nullable
        public String getUserId() { return userId; }

        @Override
        @Nullable
        public String getTenantId() { return tenantId; }

        @Override
        @NonNull
        public SessionState getState() { return state; }

        @Override
        public void setState(@NonNull SessionState sessionState) { this.state = sessionState; }

        @Override
        @NonNull
        public Instant getCreatedAt() { return createdAt; }

        @Override
        @NonNull
        public Instant getLastActiveAt() { return lastActiveAt; }

        @Override
        public void updateLastActiveAt() { this.lastActiveAt = Instant.now(); }

        @Override
        @Nullable
        public Instant getExpiresAt() { return expiresAt; }

        @Override
        @Nullable
        public String getModelName() { return modelName; }

        @Override
        @Nullable
        public String getSystemPrompt() { return systemPrompt; }

        @Override
        @NonNull
        public Map<String, String> getMetadata() { return metadata; }

        @Override
        public void setMetadata(@NonNull String key, @NonNull String value) { metadata.put(key, value); }

        @Override
        public int getMessageCount() { return messageCount; }

        public void incrementMessageCount() { this.messageCount++; }

        public void addTokens(long tokens) { this.totalTokens += tokens; }

        @Override
        public long getTotalTokens() { return totalTokens; }

        @Override
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }
}