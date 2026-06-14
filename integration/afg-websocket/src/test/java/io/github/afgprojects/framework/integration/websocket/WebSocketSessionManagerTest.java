package io.github.afgprojects.framework.integration.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.integration.websocket.handler.WebSocketSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocketSessionManager 单元测试
 *
 * <p>WebSocketSessionManager 是纯 Java 逻辑，不需要 Spring 上下文。
 * 通过直接调用方法测试会话管理逻辑。
 */
@DisplayName("WebSocketSessionManager 会话管理测试")
class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
    }

    @Nested
    @DisplayName("初始状态")
    class InitialState {

        @Test
        @DisplayName("初始在线用户数应为 0")
        void shouldHaveZeroOnlineUsers() {
            assertThat(sessionManager.getOnlineUserCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("初始在线会话数应为 0")
        void shouldHaveZeroOnlineSessions() {
            assertThat(sessionManager.getOnlineSessionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("初始在线用户集合应为空")
        void shouldHaveEmptyOnlineUsers() {
            assertThat(sessionManager.getOnlineUsers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("用户在线状态查询")
    class UserOnlineStatus {

        @Test
        @DisplayName("isUserOnline 对不在线的用户应返回 false")
        void shouldReturnFalse_forOfflineUser() {
            assertThat(sessionManager.isUserOnline("nonexistent-user")).isFalse();
        }

        @Test
        @DisplayName("getUserSessions 对不在线的用户应返回 null")
        void shouldReturnNull_forOfflineUserSessions() {
            assertThat(sessionManager.getUserSessions("nonexistent-user")).isNull();
        }
    }

    @Nested
    @DisplayName("会话查询")
    class SessionQuery {

        @Test
        @DisplayName("getSessionUser 对不存在的会话应返回 null")
        void shouldReturnNull_forNonexistentSession() {
            assertThat(sessionManager.getSessionUser("nonexistent-session")).isNull();
        }

        @Test
        @DisplayName("getSessionSubscriptions 对不存在的会话应返回 null")
        void shouldReturnNull_forNonexistentSessionSubscriptions() {
            assertThat(sessionManager.getSessionSubscriptions("nonexistent-session")).isNull();
        }
    }
}
