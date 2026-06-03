package io.github.afgprojects.framework.integration.websocket.properties.websocket;

import java.time.Duration;

import lombok.Data;

/**
 * WebSocket 会话配置。
 */
@Data
public class WebSocketSessionProperties {

    /**
     * 每个用户的最大会话数
     */
    private int maxSessionsPerUser = 5;

    /**
     * 会话超时时间
     */
    private Duration sessionTimeout = Duration.ofMinutes(30);
}