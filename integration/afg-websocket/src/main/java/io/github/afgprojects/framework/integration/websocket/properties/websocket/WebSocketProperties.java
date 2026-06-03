package io.github.afgprojects.framework.integration.websocket.properties.websocket;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * WebSocket 配置属性
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
 */
@Data
@ConfigurationProperties(prefix = "afg.websocket")
public class WebSocketProperties {

    /**
     * 是否启用 WebSocket
     */
    private boolean enabled = true;

    /**
     * WebSocket 端点路径
     */
    private String endpoint = "/ws";

    /**
     * 允许的跨域来源
     */
    private String[] allowedOrigins = {"*"};

    /**
     * 消息配置
     */
    private WebSocketMessageProperties message = new WebSocketMessageProperties();

    /**
     * 心跳配置
     */
    private WebSocketHeartbeatProperties heartbeat = new WebSocketHeartbeatProperties();

    /**
     * 会话配置
     */
    private WebSocketSessionProperties session = new WebSocketSessionProperties();
}