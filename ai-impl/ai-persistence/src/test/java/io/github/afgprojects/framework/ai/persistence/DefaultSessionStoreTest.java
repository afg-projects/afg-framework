package io.github.afgprojects.framework.ai.persistence;

import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultSessionStore 单元测试
 */
class DefaultSessionStoreTest {

    private DefaultSessionStore store;

    @BeforeEach
    void setUp() {
        store = new DefaultSessionStore();
    }

    @Test
    @DisplayName("创建会话")
    void createSession() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");

        SessionStore.Session session = store.createSession(context);

        assertThat(session.getSessionId()).isNotBlank();
        assertThat(session.getUserId()).isEqualTo("user-001");
        assertThat(session.getTenantId()).isEqualTo("tenant-001");
        assertThat(session.getState()).isEqualTo(SessionStore.SessionState.ACTIVE);
        assertThat(session.getCreatedAt()).isNotNull();
        assertThat(session.isExpired()).isFalse();
    }

    @Test
    @DisplayName("创建会话（指定 ID）")
    void createSession_withId() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");

        SessionStore.Session session = store.createSession("session-123", context);

        assertThat(session.getSessionId()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("获取会话")
    void getSession() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");
        SessionStore.Session created = store.createSession("session-123", context);

        SessionStore.Session retrieved = store.getSession("session-123");

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getSessionId()).isEqualTo("session-123");
    }

    @Test
    @DisplayName("获取不存在的会话")
    void getSession_notFound() {
        SessionStore.Session session = store.getSession("non-existent");

        assertThat(session).isNull();
    }

    @Test
    @DisplayName("更新会话")
    void updateSession() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");
        SessionStore.Session session = store.createSession("session-123", context);

        session.setState(SessionStore.SessionState.PAUSED);
        session.setMetadata("key", "value");
        store.updateSession(session);

        SessionStore.Session updated = store.getSession("session-123");

        assertThat(updated.getState()).isEqualTo(SessionStore.SessionState.PAUSED);
        assertThat(updated.getMetadata().get("key")).isEqualTo("value");
    }

    @Test
    @DisplayName("删除会话")
    void deleteSession() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");
        store.createSession("session-123", context);

        store.deleteSession("session-123");

        SessionStore.Session session = store.getSession("session-123");

        assertThat(session).isNull();
    }

    @Test
    @DisplayName("获取用户活跃会话")
    void getActiveSessions() {
        SessionStore.SessionContext context1 = createContext("user-001", "tenant-001");
        SessionStore.SessionContext context2 = createContext("user-001", "tenant-001");
        SessionStore.SessionContext context3 = createContext("user-002", "tenant-001");

        store.createSession(context1);
        store.createSession(context2);
        store.createSession(context3);

        java.util.List<SessionStore.Session> sessions = store.getActiveSessions("user-001", 10);

        assertThat(sessions).hasSize(2);
    }

    @Test
    @DisplayName("获取会话历史")
    void getSessionHistory() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");

        store.createSession(context);
        store.createSession(context);

        java.util.List<SessionStore.Session> history = store.getSessionHistory("user-001", null, null, 10);

        assertThat(history).hasSize(2);
    }

    @Test
    @DisplayName("更新最后活动时间")
    void updateLastActiveAt() {
        SessionStore.SessionContext context = createContext("user-001", "tenant-001");
        SessionStore.Session session = store.createSession(context);

        Instant before = session.getLastActiveAt();
        session.updateLastActiveAt();
        store.updateSession(session);

        SessionStore.Session updated = store.getSession(session.getSessionId());

        assertThat(updated.getLastActiveAt()).isAfterOrEqualTo(before);
    }

    @Test
    @DisplayName("会话过期")
    void sessionExpired() {
        SessionStore.SessionContext context = new SessionStore.SessionContext() {
            @Override
            public String getUserId() { return "user-001"; }
            @Override
            public String getTenantId() { return "tenant-001"; }
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public String getSystemPrompt() { return null; }
            @Override
            public Long getExpiresInSeconds() { return 1L; } // 1 秒后过期
            @Override
            public Map<String, String> getMetadata() { return Map.of(); }
        };

        SessionStore.Session session = store.createSession(context);

        assertThat(session.isExpired()).isFalse();

        // 等待过期
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            // ignore
        }

        SessionStore.Session expiredSession = store.getSession(session.getSessionId());
        assertThat(expiredSession.isExpired()).isTrue();
    }

    private SessionStore.SessionContext createContext(String userId, String tenantId) {
        return new SessionStore.SessionContext() {
            @Override
            public String getUserId() { return userId; }
            @Override
            public String getTenantId() { return tenantId; }
            @Override
            public String getModelName() { return "gpt-4"; }
            @Override
            public String getSystemPrompt() { return "You are a helpful assistant."; }
            @Override
            public Long getExpiresInSeconds() { return null; }
            @Override
            public Map<String, String> getMetadata() { return Map.of("source", "test"); }
        };
    }
}