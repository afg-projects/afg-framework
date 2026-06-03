package io.github.afgprojects.framework.integration.websocket.properties.websocket;

import lombok.Data;

/**
 * WebSocket 心跳配置。
 */
@Data
public class WebSocketHeartbeatProperties {

    /**
     * 服务端心跳间隔（毫秒），0 表示禁用
     */
    private long serverInterval = 10000;

    /**
     * 客户端心跳间隔（毫秒），0 表示禁用
     */
    private long clientInterval = 10000;
}