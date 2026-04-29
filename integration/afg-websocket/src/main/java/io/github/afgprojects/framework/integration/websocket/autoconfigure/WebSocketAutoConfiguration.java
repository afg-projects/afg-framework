package io.github.afgprojects.framework.integration.websocket.autoconfigure;

import io.github.afgprojects.framework.integration.websocket.interceptor.WebSocketInterceptorConfiguration;
import io.github.afgprojects.framework.integration.websocket.config.WebSocketConfigurer;
import io.github.afgprojects.framework.integration.websocket.config.WebSocketProperties;
import io.github.afgprojects.framework.integration.websocket.interceptor.AuthChannelInterceptor;
import io.github.afgprojects.framework.integration.websocket.interceptor.WebSocketHandshakeInterceptor;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 自动配置
 * <p>
 * 自动配置 STOMP 协议的 WebSocket 支持，提供：
 * </p>
 * <ul>
 *   <li>消息广播（/topic/*）</li>
 *   <li>点对点消息（/user/queue/*）</li>
 *   <li>认证授权支持</li>
 *   <li>心跳机制</li>
 *   <li>会话管理</li>
 * </ul>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   websocket:
 *     enabled: true
 *     endpoint: /ws
 *     allowed-origins: "*"
 *     message:
 *       buffer-size-limit: 65536
 *       time-limit-seconds: 10
 *     heartbeat:
 *       server-interval: 10000
 *       client-interval: 10000
 *     session:
 *       max-sessions-per-user: 5
 *       session-timeout: 30m
 * </pre>
 *
 * <h3>使用示例</h3>
 * <h4>1. 消息处理器</h4>
 * <pre>
 * &#064;Controller
 * public class ChatController {
 *     // 广播消息
 *     &#064;MessageMapping("/chat.broadcast")
 *     &#064;SendTo("/topic/messages")
 *     public ChatMessage broadcast(ChatMessage message) {
 *         return message;
 *     }
 *
 *     // 点对点消息
 *     &#064;MessageMapping("/chat.private")
 *     public void privateMessage(Principal principal, ChatMessage message) {
 *         messagingTemplate.convertAndSendToUser(
 *             message.getToUser(), "/queue/messages", message
 *         );
 *     }
 * }
 * </pre>
 *
 * <h4>2. 客户端连接</h4>
 * <pre>
 * // JavaScript 客户端
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, function(frame) {
 *     // 订阅广播消息
 *     stompClient.subscribe('/topic/messages', function(message) {
 *         console.log(JSON.parse(message.body));
 *     });
 *     // 订阅点对点消息
 *     stompClient.subscribe('/user/queue/messages', function(message) {
 *         console.log(JSON.parse(message.body));
 *     });
 * });
 * </pre>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WebSocketProperties.class)
@Import(WebSocketInterceptorConfiguration.class)
public class WebSocketAutoConfiguration implements WebSocketConfigurer {

    private final WebSocketProperties properties;
    private final AuthChannelInterceptor authChannelInterceptor;

    public WebSocketAutoConfiguration(
            @NonNull WebSocketProperties properties,
            @NonNull AuthChannelInterceptor authChannelInterceptor) {
        this.properties = properties;
        this.authChannelInterceptor = authChannelInterceptor;
    }

    /**
     * 配置握手拦截器
     *
     * @return 握手拦截器实例
     */
    @Bean
    @ConditionalOnMissingBean(HandshakeInterceptor.class)
    public WebSocketHandshakeInterceptor webSocketHandshakeInterceptor() {
        return new WebSocketHandshakeInterceptor();
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        WebSocketConfigurer.super.registerStompEndpoints(registry, properties);
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        WebSocketConfigurer.super.configureMessageBroker(registry, properties);
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        // 配置客户端入站通道拦截器（认证授权）
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureWebSocketTransport(@NonNull WebSocketTransportRegistration registry) {
        // 配置消息大小限制和超时时间
        registry.setMessageSizeLimit(properties.getMessage().getBufferSizeLimit())
                .setSendBufferSizeLimit(properties.getMessage().getBufferSizeLimit())
                .setSendTimeLimit(properties.getMessage().getTimeLimitSeconds() * 1000);
    }
}
