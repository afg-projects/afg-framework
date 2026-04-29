package io.github.afgprojects.framework.integration.websocket;

import io.github.afgprojects.framework.integration.websocket.config.WebSocketProperties;
import io.github.afgprojects.framework.integration.websocket.handler.WebSocketSessionManager;
import io.github.afgprojects.framework.integration.websocket.interceptor.AuthChannelInterceptor;
import io.github.afgprojects.framework.integration.websocket.interceptor.WebSocketHandshakeInterceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket 模块测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket 模块测试")
class WebSocketModuleTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketProperties properties;
    private WebSocketSessionManager sessionManager;
    private WebSocketHandshakeInterceptor handshakeInterceptor;
    private AuthChannelInterceptor authChannelInterceptor;

    @BeforeEach
    void setUp() {
        properties = new WebSocketProperties();
        sessionManager = new WebSocketSessionManager();
        handshakeInterceptor = new WebSocketHandshakeInterceptor();
        authChannelInterceptor = new AuthChannelInterceptor();
    }

    @Test
    @DisplayName("WebSocket 配置属性默认值测试")
    void testWebSocketPropertiesDefaults() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getEndpoint()).isEqualTo("/ws");
        assertThat(properties.getAllowedOrigins()).containsExactly("*");
        assertThat(properties.getMessage().getBufferSizeLimit()).isEqualTo(64 * 1024);
        assertThat(properties.getMessage().getTimeLimitSeconds()).isEqualTo(10);
        assertThat(properties.getHeartbeat().getServerInterval()).isEqualTo(10000);
        assertThat(properties.getHeartbeat().getClientInterval()).isEqualTo(10000);
        assertThat(properties.getSession().getMaxSessionsPerUser()).isEqualTo(5);
    }

    @Test
    @DisplayName("WebSocket 配置属性设置测试")
    void testWebSocketPropertiesSetters() {
        properties.setEnabled(false);
        properties.setEndpoint("/custom-ws");
        properties.setAllowedOrigins(new String[]{"https://example.com"});

        properties.getMessage().setBufferSizeLimit(128 * 1024);
        properties.getMessage().setTimeLimitSeconds(20);

        properties.getHeartbeat().setServerInterval(5000);
        properties.getHeartbeat().setClientInterval(5000);

        properties.getSession().setMaxSessionsPerUser(10);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getEndpoint()).isEqualTo("/custom-ws");
        assertThat(properties.getAllowedOrigins()).containsExactly("https://example.com");
        assertThat(properties.getMessage().getBufferSizeLimit()).isEqualTo(128 * 1024);
        assertThat(properties.getMessage().getTimeLimitSeconds()).isEqualTo(20);
        assertThat(properties.getHeartbeat().getServerInterval()).isEqualTo(5000);
        assertThat(properties.getSession().getMaxSessionsPerUser()).isEqualTo(10);
    }

    @Test
    @DisplayName("WebSocket 会话管理器初始状态测试")
    void testSessionManagerInitialState() {
        assertThat(sessionManager.getOnlineUserCount()).isZero();
        assertThat(sessionManager.getOnlineSessionCount()).isZero();
        assertThat(sessionManager.getOnlineUsers()).isEmpty();
    }

    @Test
    @DisplayName("WebSocket 握手拦截器测试")
    void testHandshakeInterceptor() {
        assertThat(handshakeInterceptor).isNotNull();
    }

    @Test
    @DisplayName("认证通道拦截器测试")
    void testAuthChannelInterceptor() {
        assertThat(authChannelInterceptor).isNotNull();
    }
}
