package io.github.afgprojects.framework.integration.websocket.config;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    private MessageConfig message = new MessageConfig();

    /**
     * 心跳配置
     */
    private HeartbeatConfig heartbeat = new HeartbeatConfig();

    /**
     * 会话配置
     */
    private SessionConfig session = new SessionConfig();

    @Data
    public static class MessageConfig {
        /**
         * 消息缓冲区大小限制（字节）
         */
        private int bufferSizeLimit = 64 * 1024;

        /**
         * 消息处理时间限制（秒）
         */
        private int timeLimitSeconds = 10;
    }

    @Data
    public static class HeartbeatConfig {
        /**
         * 服务端心跳间隔（毫秒），0 表示禁用
         */
        private long serverInterval = 10000;

        /**
         * 客户端心跳间隔（毫秒），0 表示禁用
         */
        private long clientInterval = 10000;
    }

    @Data
    public static class SessionConfig {
        /**
         * 每个用户的最大会话数
         */
        private int maxSessionsPerUser = 5;

        /**
         * 会话超时时间
         */
        private Duration sessionTimeout = Duration.ofMinutes(30);
    }
}
