package io.github.afgprojects.framework.integration.websocket.config;

import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import org.jspecify.annotations.NonNull;

/**
 * WebSocket 配置接口
 * <p>
 * 提供可扩展的 WebSocket 配置接口，业务模块可以实现此接口自定义配置。
 * </p>
 *
 * <h3>扩展示例</h3>
 * <pre>
 * &#064;Configuration
 * public class CustomWebSocketConfig implements WebSocketConfigurer {
 *     &#064;Override
 *     public void configureMessageBroker(MessageBrokerRegistry registry) {
 *         // 自定义消息代理配置
 *         registry.enableSimpleBroker("/queue", "/topic");
 *     }
 * }
 * </pre>
 */
public interface WebSocketConfigurer extends WebSocketMessageBrokerConfigurer {

    /**
     * 配置 STOMP 端点
     * <p>
     * 默认实现配置端点路径和跨域。
     * </p>
     *
     * @param registry STOMP 端点注册器
     * @param properties WebSocket 配置属性
     */
    default void registerStompEndpoints(@NonNull StompEndpointRegistry registry, @NonNull WebSocketProperties properties) {
        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOrigins())
                .withSockJS();
    }

    /**
     * 配置消息代理
     * <p>
     * 默认实现配置简单消息代理，支持 /topic（广播）和 /queue（点对点）。
     * </p>
     *
     * @param registry 消息代理注册器
     * @param properties WebSocket 配置属性
     */
    default void configureMessageBroker(@NonNull MessageBrokerRegistry registry, @NonNull WebSocketProperties properties) {
        // 启用简单消息代理
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{
                        properties.getHeartbeat().getServerInterval(),
                        properties.getHeartbeat().getClientInterval()
                });

        // 配置应用程序目的地前缀
        registry.setApplicationDestinationPrefixes("/app");

        // 配置用户目的地前缀（点对点消息）
        registry.setUserDestinationPrefix("/user");
    }
}
