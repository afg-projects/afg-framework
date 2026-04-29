package io.github.afgprojects.framework.integration.websocket.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 会话管理器测试
 */
@DisplayName("WebSocket 会话管理器测试")
class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
    }

    @Test
    @DisplayName("初始状态应该为空")
    void testInitialState() {
        assertThat(sessionManager.getOnlineUserCount()).isZero();
        assertThat(sessionManager.getOnlineSessionCount()).isZero();
        assertThat(sessionManager.getOnlineUsers()).isEmpty();
    }

    @Test
    @DisplayName("判断用户是否在线测试")
    void testIsUserOnline() {
        assertThat(sessionManager.isUserOnline("alice")).isFalse();
    }

    @Test
    @DisplayName("获取用户会话测试")
    void testGetUserSessions() {
        assertThat(sessionManager.getUserSessions("alice")).isNull();
    }

    @Test
    @DisplayName("获取会话用户测试")
    void testGetSessionUser() {
        assertThat(sessionManager.getSessionUser("session-123")).isNull();
    }

    @Test
    @DisplayName("获取会话订阅测试")
    void testGetSessionSubscriptions() {
        assertThat(sessionManager.getSessionSubscriptions("session-123")).isNull();
    }
}