package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore.SessionContext;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore.SessionState;
import io.github.afgprojects.framework.ai.core.persistence.DefaultSessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * DefaultSessionStore 纯单元测试
 */
@DisplayName("DefaultSessionStore")
class DefaultSessionStoreTest {

    private final DefaultSessionStore store = new DefaultSessionStore();

    /** 创建最小 SessionContext 桩 */
    private SessionContext context(String userId) {
        return new SessionContext() {
            @Override public String getUserId() { return userId; }
            @Override public String getTenantId() { return null; }
            @Override public String getModelName() { return "gpt-4"; }
            @Override public String getSystemPrompt() { return null; }
            @Override public Long getExpiresInSeconds() { return null; }
            @Override public Map<String, String> getMetadata() { return Map.of(); }
        };
    }

    private SessionContext contextWithExpiry(String userId, long expiresInSeconds) {
        return new SessionContext() {
            @Override public String getUserId() { return userId; }
            @Override public String getTenantId() { return null; }
            @Override public String getModelName() { return null; }
            @Override public String getSystemPrompt() { return null; }
            @Override public Long getExpiresInSeconds() { return expiresInSeconds; }
            @Override public Map<String, String> getMetadata() { return Map.of(); }
        };
    }

    @Nested
    @DisplayName("createSession + getSession")
    class CreateAndGet {

        @Test
        @DisplayName("应创建并获取会话")
        void shouldCreateAndGetSession() {
            var session = store.createSession(context("user-1"));

            var retrieved = store.getSession(session.getSessionId());

            assertThat(retrieved).isNotNull();
            assertThat(retrieved.getSessionId()).isEqualTo(session.getSessionId());
            assertThat(retrieved.getUserId()).isEqualTo("user-1");
        }

        @Test
        @DisplayName("应创建指定 ID 的会话")
        void shouldCreateSessionWithSpecificId() {
            var session = store.createSession("fixed-id", context("user-1"));

            assertThat(session.getSessionId()).isEqualTo("fixed-id");
            assertThat(store.getSession("fixed-id")).isNotNull();
        }

        @Test
        @DisplayName("不存在的会话应返回 null")
        void shouldReturnNull_whenSessionNotExists() {
            assertThat(store.getSession("nonexistent")).isNull();
        }

        @Test
        @DisplayName("新会话应为 ACTIVE 状态")
        void shouldBeActiveState() {
            var session = store.createSession(context("user-1"));

            assertThat(session.getState()).isEqualTo(SessionState.ACTIVE);
        }

        @Test
        @DisplayName("新会话应有正确的模型名称")
        void shouldHaveModelName() {
            var session = store.createSession(context("user-1"));

            assertThat(session.getModelName()).isEqualTo("gpt-4");
        }
    }

    @Nested
    @DisplayName("deleteSession")
    class DeleteSession {

        @Test
        @DisplayName("应删除会话")
        void shouldDeleteSession() {
            var session = store.createSession(context("user-1"));
            store.deleteSession(session.getSessionId());

            assertThat(store.getSession(session.getSessionId())).isNull();
        }
    }

    @Nested
    @DisplayName("updateSession")
    class UpdateSession {

        @Test
        @DisplayName("应更新会话状态")
        void shouldUpdateSessionState() {
            var session = store.createSession(context("user-1"));
            session.setState(SessionState.PAUSED);
            store.updateSession(session);

            var retrieved = store.getSession(session.getSessionId());

            assertThat(retrieved.getState()).isEqualTo(SessionState.PAUSED);
        }
    }

    @Nested
    @DisplayName("getActiveSessions")
    class GetActiveSessions {

        @Test
        @DisplayName("应获取用户的活跃会话")
        void shouldGetActiveSessions() {
            store.createSession(context("user-1"));
            store.createSession(context("user-1"));

            var activeSessions = store.getActiveSessions("user-1", 10);

            assertThat(activeSessions).hasSize(2);
            assertThat(activeSessions).allMatch(s -> s.getState() == SessionState.ACTIVE);
        }

        @Test
        @DisplayName("无活跃会话应返回空列表")
        void shouldReturnEmptyList_whenNoActiveSessions() {
            assertThat(store.getActiveSessions("user-1", 10)).isEmpty();
        }
    }

    @Nested
    @DisplayName("会话过期")
    class Expiration {

        @Test
        @DisplayName("未过期会话 isExpired 应为 false")
        void shouldNotBeExpired_whenNotExpired() {
            var session = store.createSession(contextWithExpiry("user-1", 3600));

            assertThat(session.isExpired()).isFalse();
        }

        @Test
        @DisplayName("极短过期时间后应过期")
        void shouldBeExpired_whenVeryShortExpiry() {
            // 创建 0 秒过期的会话（立即过期）
            var session = store.createSession(contextWithExpiry("user-1", 0));

            // 由于创建时间和过期时间非常接近，可能需要短暂等待
            // 但 0 秒意味着立即过期
            assertThat(session.isExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("cleanupExpiredSessions")
    class CleanupExpired {

        @Test
        @DisplayName("应清理过期会话")
        void shouldCleanupExpiredSessions() {
            // 创建一个立即过期的会话
            var session = store.createSession(contextWithExpiry("user-1", 0));

            int cleaned = store.cleanupExpiredSessions();

            // 会话已过期，应被清理
            assertThat(cleaned).isGreaterThanOrEqualTo(1);
            var retrieved = store.getSession(session.getSessionId());
            assertThat(retrieved.getState()).isEqualTo(SessionState.EXPIRED);
        }
    }

    @Nested
    @DisplayName("每用户最大会话数限制")
    class MaxSessionsPerUser {

        @Test
        @DisplayName("应限制每用户会话数量")
        void shouldLimitSessionsPerUser() {
            var limitedStore = new DefaultSessionStore(2);

            limitedStore.createSession(context("user-1"));
            limitedStore.createSession(context("user-1"));
            limitedStore.createSession(context("user-1")); // 第三个应驱逐最旧的

            var activeSessions = limitedStore.getActiveSessions("user-1", 10);

            assertThat(activeSessions).hasSize(2);
        }
    }
}
